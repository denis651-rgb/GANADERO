package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Migración de datos (no de esquema): cada animal histórico con origen=COMPRADO que todavía no
 * pertenece a ninguna Compra formal recibe una Compra migrada de un solo detalle (nunca se
 * inventa una agrupación de lote que no quedó registrada). El proveedor solo se recupera del
 * patrón exacto que ya genera IngresoLotePage.tsx ("Proveedor: <nombre>" al inicio de
 * observaciones); si no calza ese patrón, se usa un proveedor "Proveedor desconocido" explícito.
 * Es una migración Java (no .sql) porque el parseo seguro de observaciones es más claro así que
 * con funciones de texto de SQLite, y porque necesitamos generar un informe legible al terminar.
 */
public class V22__MigrarComprasHistoricas extends BaseJavaMigration {
    private static final Logger LOG = LoggerFactory.getLogger(V22__MigrarComprasHistoricas.class);
    private static final String PREFIJO_PROVEEDOR = "Proveedor: ";
    private static final String SEPARADOR = " — ";
    private static final UUID PROVEEDOR_DESCONOCIDO_ID = UUID.fromString("00000000-0000-0000-0000-00000000dead");

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        UUID proveedorDesconocidoId = asegurarProveedorDesconocido(conn);

        List<AnimalMigrable> animales = animalesSinCompra(conn);
        int migrados = 0;
        int conProveedorRecuperado = 0;
        int ambiguos = 0;

        for (AnimalMigrable a : animales) {
            String nombreProveedor = extraerProveedor(a.observaciones);
            UUID proveedorId;
            if (nombreProveedor == null) {
                proveedorId = proveedorDesconocidoId;
                if (a.observaciones != null && a.observaciones.startsWith(PREFIJO_PROVEEDOR)) ambiguos++;
            } else {
                proveedorId = buscarOCrearProveedorPorNombre(conn, nombreProveedor);
                conProveedorRecuperado++;
            }
            migrarAnimal(conn, a, proveedorId);
            migrados++;
        }

        LOG.info("Migración de compras históricas: {} animal(es) migrados ({} con proveedor recuperado, "
                + "{} con observaciones ambiguas resueltas como proveedor desconocido).",
                migrados, conProveedorRecuperado, ambiguos);
    }

    private UUID asegurarProveedorDesconocido(Connection conn) throws Exception {
        try (PreparedStatement check = conn.prepareStatement("select id from proveedor where id=?")) {
            check.setString(1, PROVEEDOR_DESCONOCIDO_ID.toString());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return PROVEEDOR_DESCONOCIDO_ID;
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "insert into proveedor(id,nombre,activo,observaciones,created_at,updated_at) "
                        + "values(?,?,1,?,?,?)")) {
            insert.setString(1, PROVEEDOR_DESCONOCIDO_ID.toString());
            insert.setString(2, "Proveedor desconocido");
            insert.setString(3, "Creado automáticamente por la migración de compras históricas: "
                    + "antecedente sin proveedor identificable.");
            String ahora = Instant.now().toString();
            insert.setString(4, ahora);
            insert.setString(5, ahora);
            insert.executeUpdate();
        }
        return PROVEEDOR_DESCONOCIDO_ID;
    }

    private String extraerProveedor(String observaciones) {
        if (observaciones == null || !observaciones.startsWith(PREFIJO_PROVEEDOR)) return null;
        String resto = observaciones.substring(PREFIJO_PROVEEDOR.length());
        int separador = resto.indexOf(SEPARADOR);
        String nombre = (separador >= 0 ? resto.substring(0, separador) : resto).trim();
        return nombre.isEmpty() ? null : nombre;
    }

    private UUID buscarOCrearProveedorPorNombre(Connection conn, String nombre) throws Exception {
        try (PreparedStatement buscar = conn.prepareStatement(
                "select id from proveedor where lower(nombre)=lower(?) limit 1")) {
            buscar.setString(1, nombre);
            try (ResultSet rs = buscar.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString(1));
            }
        }
        UUID id = UUID.randomUUID();
        try (PreparedStatement insert = conn.prepareStatement(
                "insert into proveedor(id,nombre,activo,observaciones,created_at,updated_at) "
                        + "values(?,?,1,?,?,?)")) {
            insert.setString(1, id.toString());
            insert.setString(2, nombre);
            insert.setString(3, "Recuperado automáticamente desde las observaciones de un animal migrado.");
            String ahora = Instant.now().toString();
            insert.setString(4, ahora);
            insert.setString(5, ahora);
            insert.executeUpdate();
        }
        return id;
    }

    private List<AnimalMigrable> animalesSinCompra(Connection conn) throws Exception {
        List<AnimalMigrable> resultado = new ArrayList<>();
        String sql = """
                select a.id,a.codigo,a.nombre,a.sexo,a.fecha_nacimiento,a.fecha_nacimiento_estimada,
                       a.raza_principal_id,a.categoria_actual_id,a.propiedad_actual_id,a.potrero_actual_id,
                       a.proposito,a.precio_adquisicion,a.peso_ingreso_kg,a.peso_ingreso_estimado,
                       a.fecha_ingreso,a.observaciones,a.edad_declarada_valor,a.edad_declarada_unidad,
                       a.fecha_referencia_edad,a.fuente_edad_declarada,a.observacion_estimacion
                from animal a
                where a.origen='COMPRADO'
                  and not exists(select 1 from compra_detalle cd where cd.animal_id=a.id)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new AnimalMigrable(rs.getString("id"), rs.getString("codigo"), rs.getString("nombre"),
                        rs.getString("sexo"), rs.getString("fecha_nacimiento"), rs.getInt("fecha_nacimiento_estimada"),
                        rs.getString("raza_principal_id"), rs.getString("categoria_actual_id"),
                        rs.getString("propiedad_actual_id"), rs.getString("potrero_actual_id"),
                        rs.getString("proposito"), rs.getObject("precio_adquisicion") == null ? null : rs.getBigDecimal("precio_adquisicion"),
                        rs.getObject("peso_ingreso_kg") == null ? null : rs.getBigDecimal("peso_ingreso_kg"),
                        rs.getObject("peso_ingreso_estimado") == null ? null : rs.getBoolean("peso_ingreso_estimado"),
                        rs.getString("fecha_ingreso"), rs.getString("observaciones"),
                        rs.getObject("edad_declarada_valor") == null ? null : rs.getInt("edad_declarada_valor"),
                        rs.getString("edad_declarada_unidad"), rs.getString("fecha_referencia_edad"),
                        rs.getString("fuente_edad_declarada"), rs.getString("observacion_estimacion")));
            }
        }
        return resultado;
    }

    private void migrarAnimal(Connection conn, AnimalMigrable a, UUID proveedorId) throws Exception {
        UUID compraId = UUID.randomUUID();
        String ahora = Instant.now().toString();
        java.math.BigDecimal precio = a.precioAdquisicion == null ? java.math.BigDecimal.ZERO : a.precioAdquisicion;
        String fechaRecepcion = (a.fechaIngreso == null ? ahora.substring(0, 10) : a.fechaIngreso) + "T12:00:00Z";
        String codigo = "COM-MIG-" + String.format(Locale.ROOT, "%06d", codigoSecuencial(conn));

        try (PreparedStatement insert = conn.prepareStatement("""
                insert into compra(id,codigo,proveedor_id,fecha_recepcion,modalidad_precio,moneda,
                    cantidad_animales,precio_unitario,precio_total,precio_unitario_referencial,
                    propiedad_id,potrero_id,proposito,observaciones,estado,origen_migracion,
                    created_at,updated_at)
                values(?,?,?,?,'POR_UNIDAD','BOB',1,?,?,?,?,?,?,?,'CONFIRMADA','MIGRADO',?,?)
                """)) {
            insert.setString(1, compraId.toString());
            insert.setString(2, codigo);
            insert.setString(3, proveedorId.toString());
            insert.setString(4, fechaRecepcion);
            insert.setBigDecimal(5, precio);
            insert.setBigDecimal(6, precio);
            insert.setBigDecimal(7, precio);
            insert.setString(8, a.propiedadActualId);
            insert.setString(9, a.potreroActualId);
            insert.setString(10, a.proposito);
            insert.setString(11, "Compra histórica migrada automáticamente a partir del registro existente del animal "
                    + a.codigo + ".");
            insert.setString(12, ahora);
            insert.setString(13, ahora);
            insert.executeUpdate();
        }

        try (PreparedStatement insert = conn.prepareStatement("""
                insert into compra_detalle(id,compra_id,animal_id,numero_linea,precio_asignado,peso_ingreso_kg,
                    tipo_peso,propiedad_id,potrero_id,codigo_solicitado,nombre,sexo,raza_id,proposito,
                    fecha_nacimiento,fecha_nacimiento_estimada,edad_declarada_valor,edad_declarada_unidad,
                    fecha_referencia_edad,fuente_edad_declarada,observacion_estimacion,categoria_actual_id,
                    observaciones,created_at)
                values(?,?,?,1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            String tipoPeso = a.pesoIngresoKg == null || a.pesoIngresoEstimado == null ? null
                    : (a.pesoIngresoEstimado ? "ESTIMADO" : "MEDIDO");
            insert.setString(1, UUID.randomUUID().toString());
            insert.setString(2, compraId.toString());
            insert.setString(3, a.id);
            insert.setBigDecimal(4, precio);
            if (a.pesoIngresoKg == null) insert.setNull(5, java.sql.Types.NUMERIC); else insert.setBigDecimal(5, a.pesoIngresoKg);
            insert.setString(6, tipoPeso);
            insert.setString(7, a.propiedadActualId);
            insert.setString(8, a.potreroActualId);
            insert.setString(9, a.codigo);
            insert.setString(10, a.nombre);
            insert.setString(11, a.sexo);
            insert.setString(12, a.razaPrincipalId);
            insert.setString(13, a.proposito);
            insert.setString(14, a.fechaNacimiento);
            insert.setInt(15, a.fechaNacimientoEstimada);
            if (a.edadDeclaradaValor == null) insert.setNull(16, java.sql.Types.INTEGER); else insert.setInt(16, a.edadDeclaradaValor);
            insert.setString(17, a.edadDeclaradaUnidad);
            insert.setString(18, a.fechaReferenciaEdad);
            insert.setString(19, a.fuenteEdadDeclarada);
            insert.setString(20, a.observacionEstimacion);
            insert.setString(21, a.categoriaActualId);
            insert.setString(22, "Detalle migrado automáticamente. Observaciones originales conservadas en el animal.");
            insert.setString(23, ahora);
            insert.executeUpdate();

            if (a.pesoIngresoKg != null) {
                try (PreparedStatement pesajeInsert = conn.prepareStatement("""
                        insert into pesaje(id,animal_id,fecha,peso_kg,tipo,tipo_peso,potrero_id,compra_id,
                            estado,observaciones,created_at,updated_at)
                        values(?,?,?,?,'COMPRA',?,?,?,'ACTIVO',?,?,?)
                        """)) {
                    pesajeInsert.setString(1, UUID.randomUUID().toString());
                    pesajeInsert.setString(2, a.id);
                    pesajeInsert.setString(3, a.fechaIngreso == null ? ahora.substring(0, 10) : a.fechaIngreso);
                    pesajeInsert.setBigDecimal(4, a.pesoIngresoKg);
                    pesajeInsert.setString(5, tipoPeso);
                    pesajeInsert.setString(6, a.potreroActualId);
                    pesajeInsert.setString(7, compraId.toString());
                    pesajeInsert.setString(8, tipoPeso == null
                            ? "Migrado desde el peso al ingreso del animal. Tipo (medido/estimado) desconocido: "
                                    + "no se registró en el dato original."
                            : "Migrado desde el peso al ingreso del animal.");
                    pesajeInsert.setString(9, ahora);
                    pesajeInsert.setString(10, ahora);
                    pesajeInsert.executeUpdate();
                }
            }
        }
    }

    private int codigoSecuencial(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "select count(*) from compra where origen_migracion='MIGRADO'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) + 1;
        }
    }

    private record AnimalMigrable(String id, String codigo, String nombre, String sexo, String fechaNacimiento,
                                  int fechaNacimientoEstimada, String razaPrincipalId, String categoriaActualId,
                                  String propiedadActualId, String potreroActualId, String proposito,
                                  java.math.BigDecimal precioAdquisicion, java.math.BigDecimal pesoIngresoKg,
                                  Boolean pesoIngresoEstimado, String fechaIngreso, String observaciones,
                                  Integer edadDeclaradaValor, String edadDeclaradaUnidad, String fechaReferenciaEdad,
                                  String fuenteEdadDeclarada, String observacionEstimacion) {
    }
}
