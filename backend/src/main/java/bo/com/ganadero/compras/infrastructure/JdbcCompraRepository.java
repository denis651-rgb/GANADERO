package bo.com.ganadero.compras.infrastructure;

import bo.com.ganadero.animales.domain.FuenteEdadDeclarada;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.animales.domain.UnidadEdadDeclarada;
import bo.com.ganadero.compras.domain.*;
import bo.com.ganadero.pesajes.domain.TipoPeso;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
class JdbcCompraRepository implements CompraRepository {
    private final JdbcClient jdbc;

    JdbcCompraRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public CompraPage list(EstadoCompra estado, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (estado != null) {
            where.append(" and estado=:estado");
            params.put("estado", estado.name());
        }
        long total = jdbc.sql("select count(*) from compra" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Compra> content = jdbc.sql("select * from compra" + where + " order by fecha_recepcion desc, created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return CompraPage.of(content, page, size, total);
    }

    public Optional<Compra> findById(UUID id) {
        return jdbc.sql("select * from compra where id=:id").param("id", id.toString()).query(this::map).optional();
    }

    public Optional<Compra> findByAnimalId(UUID animalId) {
        return jdbc.sql("select c.* from compra c join compra_detalle d on d.compra_id=c.id where d.animal_id=:a")
                .param("a", animalId.toString()).query(this::map).optional();
    }

    public List<CompraDetalle> findDetalles(UUID compraId) {
        return jdbc.sql("select * from compra_detalle where compra_id=:c order by numero_linea")
                .param("c", compraId.toString()).query(this::mapDetalle).list();
    }

    public Compra crear(Compra c, List<CompraDetalle> detalles, UUID actor) {
        jdbc.sql("""
                insert into compra(id,codigo,proveedor_id,fecha_recepcion,modalidad_precio,moneda,cantidad_animales,
                    precio_unitario,precio_total,precio_unitario_referencial,propiedad_id,potrero_id,lote_ganadero_id,
                    proposito,observaciones,estado,origen_migracion,created_by,updated_by)
                values(:id,:codigo,:proveedor,:fecha,:modalidad,:moneda,:cantidad,:unitario,:total,:referencial,
                    :propiedad,:potrero,:lote,:proposito,:observaciones,:estado,:origenMigracion,:actor,:actor)
                """).params(paramsCompra(c, actor)).update();
        insertarDetalles(detalles);
        return findById(c.id()).orElseThrow();
    }

    public Compra actualizarBorrador(Compra c, List<CompraDetalle> detalles, UUID actor) {
        int changed = jdbc.sql("""
                update compra set proveedor_id=:proveedor,fecha_recepcion=:fecha,modalidad_precio=:modalidad,
                    moneda=:moneda,cantidad_animales=:cantidad,precio_unitario=:unitario,precio_total=:total,
                    precio_unitario_referencial=:referencial,propiedad_id=:propiedad,potrero_id=:potrero,
                    lote_ganadero_id=:lote,proposito=:proposito,observaciones=:observaciones,
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='BORRADOR'
                """).params(paramsCompra(c, actor)).update();
        if (changed == 0) throw missingOrConflict(c.id());
        jdbc.sql("delete from compra_detalle where compra_id=:c").param("c", c.id().toString()).update();
        insertarDetalles(detalles);
        return findById(c.id()).orElseThrow();
    }

    public void asignarAnimal(UUID detalleId, UUID animalId) {
        jdbc.sql("update compra_detalle set animal_id=:animal where id=:id")
                .param("animal", animalId.toString()).param("id", detalleId.toString()).update();
    }

    public Compra confirmar(UUID id, long version, UUID actor) {
        int changed = jdbc.sql("""
                update compra set estado='CONFIRMADA',updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='BORRADOR'
                """).param("actor", actor.toString()).param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id);
        return findById(id).orElseThrow();
    }

    public Compra anular(UUID id, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update compra set estado='ANULADA',motivo_anulacion=:motivo,anulado_por=:actor,
                    fecha_anulacion=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='CONFIRMADA'
                """).param("motivo", motivo).param("actor", actor.toString())
                .param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id);
        return findById(id).orElseThrow();
    }

    public List<DependenciaCompra> dependenciasPosteriores(UUID animalId, UUID compraId) {
        List<DependenciaCompra> resultado = new ArrayList<>();
        String animal = animalId.toString();
        record Check(String sql, String etiqueta) {}
        List<Check> checks = List.of(
                new Check("select count(*) from movimiento_detalle md join movimiento m on m.id=md.movimiento_id "
                        + "where md.animal_id=:a and m.tipo<>'INGRESO_COMPRA'", "Movimiento posterior"),
                new Check("select count(*) from pesaje where animal_id=:a and estado='ACTIVO' "
                        + "and (compra_id is null or compra_id<>:c)", "Pesaje posterior"),
                new Check("select count(*) from venta where animal_id=:a", "Venta registrada"),
                new Check("select count(*) from aplicacion_sanitaria where animal_id=:a", "Aplicación sanitaria"),
                new Check("select count(*) from celo where animal_id=:a", "Celo registrado"),
                new Check("select count(*) from servicio where hembra_id=:a", "Servicio reproductivo"),
                new Check("select count(*) from diagnostico_gestacion where animal_id=:a", "Diagnóstico de gestación"),
                new Check("select count(*) from parto where madre_id=:a", "Parto registrado"),
                new Check("select count(*) from aborto where animal_id=:a", "Aborto registrado"),
                new Check("select count(*) from destete where animal_cria_id=:a or madre_id=:a", "Destete registrado"),
                new Check("select count(*) from animal where id=:a and estado<>'ACTIVO'", "El animal ya no está activo")
        );
        for (Check check : checks) {
            long count = jdbc.sql(check.sql()).param("a", animal).param("c", compraId.toString()).query(Long.class).single();
            if (count > 0) resultado.add(new DependenciaCompra(null, check.etiqueta(), count + " registro(s)"));
        }
        return resultado;
    }

    private void insertarDetalles(List<CompraDetalle> detalles) {
        for (CompraDetalle d : detalles) {
            jdbc.sql("""
                    insert into compra_detalle(id,compra_id,animal_id,numero_linea,precio_asignado,peso_ingreso_kg,
                        tipo_peso,metodo_peso,propiedad_id,potrero_id,lote_ganadero_id,codigo_solicitado,nombre,sexo,raza_id,
                        proposito,fecha_nacimiento,fecha_nacimiento_estimada,edad_declarada_valor,
                        edad_declarada_unidad,fecha_referencia_edad,fuente_edad_declarada,observacion_estimacion,
                        categoria_actual_id,categoria_manual_motivo,observaciones)
                    values(:id,:compra,:animal,:linea,:precio,:peso,:tipoPeso,:metodoPeso,:propiedad,:potrero,:lote,
                        :codigoSolicitado,:nombre,:sexo,:raza,:proposito,:fechaNacimiento,:fechaNacimientoEstimada,
                        :edadValor,:edadUnidad,:edadReferencia,:edadFuente,:edadDetalle,:categoria,:categoriaMotivo,
                        :observaciones)
                    """)
                    .param("id", d.id().toString()).param("compra", d.compraId().toString())
                    .param("animal", d.animalId() == null ? null : d.animalId().toString())
                    .param("linea", d.numeroLinea()).param("precio", d.precioAsignado())
                    .param("peso", d.pesoIngresoKg()).param("tipoPeso", d.tipoPeso() == null ? null : d.tipoPeso().name())
                    .param("metodoPeso", d.metodoPeso())
                    .param("propiedad", d.propiedadId().toString()).param("potrero", d.potreroId().toString())
                    .param("lote", d.loteGanaderoId() == null ? null : d.loteGanaderoId().toString())
                    .param("codigoSolicitado", d.codigoSolicitado()).param("nombre", d.nombre())
                    .param("sexo", d.sexo() == null ? null : d.sexo().name())
                    .param("raza", d.razaId() == null ? null : d.razaId().toString())
                    .param("proposito", d.proposito() == null ? null : d.proposito().name())
                    .param("fechaNacimiento", d.fechaNacimiento() == null ? null : d.fechaNacimiento().toString())
                    .param("fechaNacimientoEstimada", d.fechaNacimientoEstimada() ? 1 : 0)
                    .param("edadValor", d.edadDeclaradaValor())
                    .param("edadUnidad", d.edadDeclaradaUnidad() == null ? null : d.edadDeclaradaUnidad().name())
                    .param("edadReferencia", d.fechaReferenciaEdad() == null ? null : d.fechaReferenciaEdad().toString())
                    .param("edadFuente", d.fuenteEdadDeclarada() == null ? null : d.fuenteEdadDeclarada().name())
                    .param("edadDetalle", d.observacionEstimacion())
                    .param("categoria", d.categoriaActualId() == null ? null : d.categoriaActualId().toString())
                    .param("categoriaMotivo", d.categoriaManualMotivo())
                    .param("observaciones", d.observaciones())
                    .update();
        }
    }

    private Map<String, Object> paramsCompra(Compra c, UUID actor) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", c.id().toString());
        p.put("codigo", c.codigo());
        p.put("proveedor", c.proveedorId().toString());
        p.put("fecha", c.fechaRecepcion().toString());
        p.put("modalidad", c.modalidad().name());
        p.put("moneda", c.moneda());
        p.put("cantidad", c.cantidadAnimales());
        p.put("unitario", c.precioUnitario());
        p.put("total", c.precioTotal());
        p.put("referencial", c.precioUnitarioReferencial());
        p.put("propiedad", c.propiedadId().toString());
        p.put("potrero", c.potreroId().toString());
        p.put("lote", c.loteGanaderoId() == null ? null : c.loteGanaderoId().toString());
        p.put("proposito", c.proposito() == null ? null : c.proposito().name());
        p.put("observaciones", c.observaciones());
        p.put("estado", c.estado().name());
        p.put("origenMigracion", c.origenMigracion());
        p.put("actor", actor.toString());
        p.put("version", c.version());
        return p;
    }

    private BusinessException missingOrConflict(UUID id) {
        boolean exists = findById(id).isPresent();
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.COMPRA_NOT_FOUND);
    }

    private Compra map(ResultSet r, int row) throws SQLException {
        String modalidad = r.getString("modalidad_precio");
        String estado = r.getString("estado");
        return new Compra(Rows.uuid(r, "id"), r.getString("codigo"), Rows.uuid(r, "proveedor_id"),
                Rows.instant(r, "fecha_recepcion"), modalidad == null ? null : bo.com.ganadero.compras.domain.ModalidadPrecio.valueOf(modalidad),
                r.getString("moneda"), r.getInt("cantidad_animales"), r.getBigDecimal("precio_unitario"),
                r.getBigDecimal("precio_total"), r.getBigDecimal("precio_unitario_referencial"),
                Rows.uuid(r, "propiedad_id"), Rows.uuid(r, "potrero_id"), Rows.uuid(r, "lote_ganadero_id"),
                r.getString("proposito") == null ? null : bo.com.ganadero.animales.domain.PropositoAnimal.valueOf(r.getString("proposito")),
                r.getString("observaciones"),
                estado == null ? null : EstadoCompra.valueOf(estado), r.getString("motivo_anulacion"),
                Rows.uuid(r, "anulado_por"), Rows.instant(r, "fecha_anulacion"), r.getString("origen_migracion"),
                Rows.instant(r, "created_at"), Rows.uuid(r, "created_by"), Rows.instant(r, "updated_at"),
                Rows.uuid(r, "updated_by"), r.getLong("version"));
    }

    private CompraDetalle mapDetalle(ResultSet r, int row) throws SQLException {
        String sexo = r.getString("sexo");
        String tipoPeso = r.getString("tipo_peso");
        String edadUnidad = r.getString("edad_declarada_unidad");
        String fuenteEdad = r.getString("fuente_edad_declarada");
        return new CompraDetalle(Rows.uuid(r, "id"), Rows.uuid(r, "compra_id"), Rows.uuid(r, "animal_id"),
                r.getInt("numero_linea"), r.getBigDecimal("precio_asignado"), r.getBigDecimal("peso_ingreso_kg"),
                tipoPeso == null ? null : TipoPeso.valueOf(tipoPeso), r.getString("metodo_peso"), Rows.uuid(r, "propiedad_id"),
                Rows.uuid(r, "potrero_id"), Rows.uuid(r, "lote_ganadero_id"), r.getString("codigo_solicitado"),
                r.getString("nombre"), sexo == null ? null : SexoAnimal.valueOf(sexo), Rows.uuid(r, "raza_id"),
                r.getString("proposito") == null ? null : bo.com.ganadero.animales.domain.PropositoAnimal.valueOf(r.getString("proposito")),
                r.getString("fecha_nacimiento") == null ? null : java.time.LocalDate.parse(r.getString("fecha_nacimiento")),
                r.getBoolean("fecha_nacimiento_estimada"), (Integer) r.getObject("edad_declarada_valor"),
                edadUnidad == null ? null : UnidadEdadDeclarada.valueOf(edadUnidad),
                r.getString("fecha_referencia_edad") == null ? null : java.time.LocalDate.parse(r.getString("fecha_referencia_edad")),
                fuenteEdad == null ? null : FuenteEdadDeclarada.valueOf(fuenteEdad), r.getString("observacion_estimacion"),
                Rows.uuid(r, "categoria_actual_id"), r.getString("categoria_manual_motivo"),
                r.getString("observaciones"), Rows.instant(r, "created_at"));
    }
}
