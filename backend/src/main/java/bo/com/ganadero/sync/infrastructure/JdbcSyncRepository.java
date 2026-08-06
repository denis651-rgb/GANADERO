package bo.com.ganadero.sync.infrastructure;

import bo.com.ganadero.sync.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcSyncRepository implements SyncRepository {
    private final JdbcClient jdbc;

    public JdbcSyncRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Dispositivo upsertDispositivo(Dispositivo d) {
        jdbc.sql("""
                insert into sync.dispositivos(id,empresa_id,usuario_id,codigo_dispositivo,nombre,plataforma,version_app,
                estado,ultimo_seen_at,ultimo_cursor)
                values(:id,:empresa,:usuario,:codigo,:nombre,:plataforma,:version,'ACTIVO',now(),:cursor)
                on conflict (empresa_id, codigo_dispositivo) do update set
                usuario_id=excluded.usuario_id,nombre=coalesce(excluded.nombre,sync.dispositivos.nombre),
                plataforma=excluded.plataforma,version_app=excluded.version_app,ultimo_seen_at=now(),
                ultimo_cursor=greatest(sync.dispositivos.ultimo_cursor,excluded.ultimo_cursor),updated_at=now()
                """).param("id", d.id()).param("empresa", d.empresaId()).param("usuario", d.usuarioId())
                .param("codigo", d.codigoDispositivo()).param("nombre", d.nombre())
                .param("plataforma", d.plataforma() == null ? "WEB" : d.plataforma())
                .param("version", d.versionApp()).param("cursor", d.ultimoCursor()).update();
        return findDispositivo(d.empresaId(), d.codigoDispositivo()).orElseThrow();
    }

    @Override
    public Optional<Dispositivo> findDispositivo(UUID empresa, String codigoDispositivo) {
        return jdbc.sql("select * from sync.dispositivos where empresa_id=:empresa and codigo_dispositivo=:codigo")
                .param("empresa", empresa).param("codigo", codigoDispositivo).query(this::mapDispositivo).optional();
    }

    @Override
    public Optional<OperacionSync> findOperacion(UUID empresa, UUID dispositivoId, UUID clienteId) {
        return jdbc.sql("""
                select * from sync.operaciones
                where empresa_id=:empresa and dispositivo_id=:dispositivo and cliente_id=:cliente
                """).param("empresa", empresa).param("dispositivo", dispositivoId).param("cliente", clienteId)
                .query(this::mapOperacion).optional();
    }

    @Override
    public OperacionSync saveOperacion(OperacionSync op) {
        jdbc.sql("""
                insert into sync.operaciones(id,empresa_id,dispositivo_id,usuario_id,cliente_id,tipo,entidad,entidad_id,
                datos,version_cliente,estado,resultado_codigo,resultado_mensaje,resultado_servidor,version_servidor,
                conflictos,idempotency_key,created_at,applied_at)
                values(:id,:empresa,:dispositivo,:usuario,:cliente,:tipo,:entidad,:entidadId,:datos,:versionCliente,
                :estado,:codigo,:mensaje,:servidor,:versionServidor,:conflictos,:idempotency,now(),:appliedAt)
                on conflict (empresa_id, dispositivo_id, cliente_id) do update set
                estado=excluded.estado,resultado_codigo=excluded.resultado_codigo,
                resultado_mensaje=excluded.resultado_mensaje,resultado_servidor=excluded.resultado_servidor,
                version_servidor=excluded.version_servidor,conflictos=excluded.conflictos,applied_at=excluded.applied_at
                """).param("id", op.id()).param("empresa", op.empresaId()).param("dispositivo", op.dispositivoId())
                .param("usuario", op.usuarioId()).param("cliente", op.clienteId()).param("tipo", op.tipo())
                .param("entidad", op.entidad() == null ? "" : op.entidad()).param("entidadId", op.entidadId())
                .param("datos", op.datosJson()).param("versionCliente", op.versionCliente())
                .param("estado", op.estado()).param("codigo", op.resultadoCodigo())
                .param("mensaje", op.resultadoMensaje()).param("servidor", op.resultadoServidorJson())
                .param("versionServidor", op.versionServidor()).param("conflictos", op.conflictosJson())
                .param("idempotency", op.idempotencyKey())
                .param("appliedAt", op.appliedAt() == null ? null : java.sql.Timestamp.from(op.appliedAt())).update();
        return findOperacion(op.empresaId(), op.dispositivoId(), op.clienteId()).orElseThrow();
    }

    @Override
    public void setDispositivoOrigen(String codigoDispositivo) {
        jdbc.sql("select set_config('sync.dispositivo', :codigo, true)")
                .param("codigo", codigoDispositivo == null ? "" : codigoDispositivo).query(String.class).single();
    }

    @Override
    public List<CambioSync> pullCambios(UUID empresa, long cursor, int size) {
        return jdbc.sql("""
                select id,empresa_id,tabla,entidad_id,tipo_cambio,datos::text as datos,dispositivo_origen,created_at
                from sync.cambios where empresa_id=:empresa and id>:cursor order by id limit :size
                """).param("empresa", empresa).param("cursor", cursor).param("size", size)
                .query(this::mapCambio).list();
    }

    @Override
    public boolean hasCambiosDespues(UUID empresa, long cursor) {
        return jdbc.sql("select exists(select 1 from sync.cambios where empresa_id=:empresa and id>:cursor)")
                .param("empresa", empresa).param("cursor", cursor).query(Boolean.class).single();
    }

    @Override
    public long ultimoCursor(UUID empresa) {
        Long max = jdbc.sql("select max(id) from sync.cambios where empresa_id=:empresa")
                .param("empresa", empresa).query(Long.class).single();
        return max == null ? 0 : max;
    }

    @Override
    public List<Map<String, Object>> bootstrapEmpresas(UUID empresa) {
        return jdbc.sql("select id, codigo, razon_social as \"razonSocial\", nombre_comercial as \"nombreComercial\", " +
                "moneda, zona_horaria as \"zonaHoraria\", estado from core.empresas where id=:e")
                .param("e", empresa).query(this::toMap).list();
    }

    @Override
    public List<Map<String, Object>> bootstrapPropiedades(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select id, empresa_id as \"empresaId\", codigo, nombre, departamento, municipio, " +
                "superficie_ha as \"superficieHa\", activo, version from core.propiedades where empresa_id=:e",
                "id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapSectores(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select id, empresa_id as \"empresaId\", propiedad_id as \"propiedadId\", codigo, nombre, " +
                "activo, version from campo.sectores where empresa_id=:e", "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapPotreros(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select id, empresa_id as \"empresaId\", propiedad_id as \"propiedadId\", " +
                "sector_id as \"sectorId\", codigo, nombre, superficie_ha as \"superficieHa\", tipo_pasto_id as \"tipoPastoId\", " +
                "capacidad_ua as \"capacidadUa\", tiene_agua as \"tieneAgua\", estado, activo, version " +
                "from campo.potreros where empresa_id=:e", "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapLotes(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select id, empresa_id as \"empresaId\", propiedad_id as \"propiedadId\", codigo, nombre, " +
                "descripcion, estado, fecha_apertura as \"fechaApertura\", fecha_cierre as \"fechaCierre\", version " +
                "from ganado.lotes_ganaderos where empresa_id=:e", "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapRazas(UUID empresa) {
        return jdbc.sql("select id, empresa_id as \"empresaId\", codigo, nombre, especie, descripcion, activo " +
                "from ganado.razas where empresa_id is null or empresa_id=:e").param("e", empresa).query(this::toMap).list();
    }

    @Override
    public List<Map<String, Object>> bootstrapCategorias(UUID empresa) {
        return jdbc.sql("select id, empresa_id as \"empresaId\", codigo, nombre, sexo_aplicable as \"sexoAplicable\", " +
                "edad_min_meses as \"edadMinMeses\", edad_max_meses as \"edadMaxMeses\", descripcion, activo " +
                "from ganado.categorias_animal where empresa_id is null or empresa_id=:e")
                .param("e", empresa).query(this::toMap).list();
    }

    @Override
    public List<Map<String, Object>> bootstrapTiposPasto(UUID empresa) {
        return jdbc.sql("select id, empresa_id as \"empresaId\", codigo, nombre, activo " +
                "from campo.tipos_pasto where empresa_id is null or empresa_id=:e")
                .param("e", empresa).query(this::toMap).list();
    }

    @Override
    public List<Map<String, Object>> bootstrapAnimales(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select id, empresa_id as \"empresaId\", codigo, nombre, sexo, " +
                "fecha_nacimiento as \"fechaNacimiento\", fecha_nacimiento_estimada as \"fechaNacimientoEstimada\", " +
                "raza_principal_id as \"razaPrincipalId\", categoria_actual_id as \"categoriaActualId\", color, proposito, origen, " +
                "propiedad_actual_id as \"propiedadActualId\", potrero_actual_id as \"potreroActualId\", " +
                "lote_actual_id as \"loteActualId\", estado, fecha_ingreso as \"fechaIngreso\", " +
                "precio_adquisicion as \"precioAdquisicion\", peso_nacimiento_kg as \"pesoNacimientoKg\", " +
                "condicion_corporal_actual as \"condicionCorporalActual\", foto_principal_path as \"fotoPrincipalPath\", " +
                "observaciones, version from ganado.animales where empresa_id=:e",
                "propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapIdentificadores(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select i.id, i.empresa_id as \"empresaId\", i.animal_id as \"animalId\", i.tipo, i.valor, " +
                "i.principal, i.estado, i.fecha_asignacion as \"fechaAsignacion\", i.fecha_retiro as \"fechaRetiro\", " +
                "i.observaciones, i.version from ganado.identificadores_animal i " +
                "join ganado.animales a on a.id=i.animal_id where i.empresa_id=:e",
                "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapPesajes(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select p.id, p.empresa_id as \"empresaId\", p.animal_id as \"animalId\", p.fecha, " +
                "p.peso_kg as \"pesoKg\", p.tipo, p.condicion_corporal as \"condicionCorporal\", p.bascula, " +
                "p.responsable_id as \"responsableId\", p.propiedad_id as \"propiedadId\", p.potrero_id as \"potreroId\", " +
                "p.lote_id as \"loteId\", p.dispositivo, p.cliente_uuid as \"clienteUuid\", p.idempotency_key as \"idempotencyKey\", " +
                "p.estado, p.observaciones, p.version from produccion.pesajes p " +
                "join ganado.animales a on a.id=p.animal_id where p.empresa_id=:e",
                "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public List<Map<String, Object>> bootstrapMembresias(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return list("select m.id, m.empresa_id as \"empresaId\", m.lote_id as \"loteId\", " +
                "m.animal_id as \"animalId\", m.fecha_ingreso as \"fechaIngreso\", m.fecha_salida as \"fechaSalida\", " +
                "m.motivo_salida as \"motivoSalida\" from ganado.membresias_lote m " +
                "join ganado.animales a on a.id=m.animal_id where m.empresa_id=:e",
                "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    private List<Map<String, Object>> list(String base, String propiedadColumna, UUID empresa,
                                           boolean todas, Set<UUID> permitidas) {
        if (!todas && permitidas.isEmpty()) return List.of();
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todas) {
            base += " and " + propiedadColumna + " in (:allowed)";
            params.put("allowed", permitidas);
        }
        return jdbc.sql(base).params(params).query(this::toMap).list();
    }

    private Map<String, Object> toMap(ResultSet rs, int row) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Object value = rs.getObject(i);
            if (value instanceof Timestamp timestamp) value = timestamp.toInstant();
            else if (value instanceof Date date) value = date.toLocalDate();
            map.put(meta.getColumnLabel(i), value);
        }
        return map;
    }

    private Dispositivo mapDispositivo(ResultSet r, int row) throws SQLException {
        return new Dispositivo(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("usuario_id", UUID.class), r.getString("codigo_dispositivo"),
                r.getString("nombre"), r.getString("plataforma"), r.getString("version_app"),
                EstadoDispositivo.valueOf(r.getString("estado")),
                r.getTimestamp("ultimo_seen_at").toInstant(), r.getLong("ultimo_cursor"), r.getLong("version"));
    }

    private OperacionSync mapOperacion(ResultSet r, int row) throws SQLException {
        Timestamp appliedAt = r.getTimestamp("applied_at");
        Timestamp createdAt = r.getTimestamp("created_at");
        Long versionServidor = r.getObject("version_servidor") == null ? null : r.getLong("version_servidor");
        return new OperacionSync(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("dispositivo_id", UUID.class), r.getObject("usuario_id", UUID.class),
                r.getObject("cliente_id", UUID.class), r.getString("tipo"), r.getString("entidad"),
                r.getObject("entidad_id", UUID.class), r.getString("datos"), r.getLong("version_cliente"),
                r.getString("estado"), r.getString("resultado_codigo"), r.getString("resultado_mensaje"),
                r.getString("resultado_servidor"), versionServidor, r.getString("conflictos"),
                r.getString("idempotency_key"), createdAt == null ? null : createdAt.toInstant(),
                appliedAt == null ? null : appliedAt.toInstant());
    }

    private CambioSync mapCambio(ResultSet r, int row) throws SQLException {
        Timestamp createdAt = r.getTimestamp("created_at");
        return new CambioSync(r.getLong("id"), r.getObject("empresa_id", UUID.class), r.getString("tabla"),
                r.getObject("entidad_id", UUID.class), r.getString("tipo_cambio"), r.getString("datos"),
                r.getString("dispositivo_origen"), createdAt == null ? null : createdAt.toInstant());
    }
}
