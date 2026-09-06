package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSanidadRepository implements SanidadRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcSanidadRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public List<Enfermedad> enfermedades(UUID e, boolean all) {
        return jdbc.sql("select * from enfermedad where 1=1" + (all ? "" : " and activo") + " order by nombre")
                .query(this::enfermedad).list();
    }

    @Override
    public Optional<Enfermedad> enfermedad(UUID id, UUID e) {
        return jdbc.sql("select * from enfermedad where id=:id").param("id", id.toString()).query(this::enfermedad).optional();
    }

    @Override
    public Enfermedad crearEnfermedad(Enfermedad v) {
        jdbc.sql("insert into enfermedad(id,codigo,nombre,descripcion,es_notificable,activo) values(:id,:c,:n,:d,:not,:a)")
                .param("id", v.id().toString()).param("c", v.codigo()).param("n", v.nombre()).param("d", v.descripcion())
                .param("not", v.esNotificable()).param("a", v.activo()).update();
        return enfermedad(v.id(), v.empresaId()).orElseThrow();
    }

    @Override
    public Enfermedad cambiarEstadoEnfermedad(UUID id, UUID e, boolean a) {
        int n = jdbc.sql("update enfermedad set activo=:a,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now') where id=:id")
                .param("a", a).param("id", id.toString()).update();
        if (n == 0) throw new BusinessException(ErrorCode.SANIDAD_ENFERMEDAD_NOT_FOUND);
        return enfermedad(id, e).orElseThrow();
    }

    @Override
    public List<PlanSanitario> planes(UUID e) {
        return jdbc.sql("select * from plan_sanitario order by fecha_inicio desc").query(this::plan).list();
    }

    @Override
    public Optional<PlanSanitario> plan(UUID id, UUID e) {
        return jdbc.sql("select * from plan_sanitario where id=:id").param("id", id.toString()).query(this::plan).optional();
    }

    @Override
    public PlanSanitario crearPlan(PlanSanitario v, UUID a) {
        jdbc.sql("""
                insert into plan_sanitario(id,nombre,descripcion,fecha_inicio,fecha_fin,estado,propiedad_id,created_by,updated_by)
                values(:id,:n,:d,:ini,:fin,:estado,:prop,:a,:a)
                """)
                .param("id", v.id().toString()).param("n", v.nombre()).param("d", v.descripcion())
                .param("ini", v.fechaInicio() == null ? null : v.fechaInicio().toString())
                .param("fin", v.fechaFin() == null ? null : v.fechaFin().toString())
                .param("estado", v.estado().name())
                .param("prop", v.propiedadId() == null ? null : v.propiedadId().toString())
                .param("a", a.toString()).update();
        return plan(v.id(), v.empresaId()).orElseThrow();
    }

    @Override
    public PlanSanitario cambiarEstadoPlan(UUID id, UUID e, EstadoPlanSanitario estado, long version, UUID actor) {
        int n = jdbc.sql("""
                update plan_sanitario set estado=:s,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:a,version=version+1
                where id=:id and version=:v
                """)
                .param("s", estado.name()).param("a", actor.toString()).param("id", id.toString()).param("v", version).update();
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return plan(id, e).orElseThrow();
    }

    @Override
    public List<PlanSanitario> planesActivosEnAlcance(UUID propiedadId, UUID empresa, UUID excluirPlanId) {
        String sql = "select * from plan_sanitario where estado='ACTIVO' and id<>:excluir and "
                + (propiedadId == null ? "propiedad_id is null" : "propiedad_id=:prop");
        var q = jdbc.sql(sql).param("excluir", excluirPlanId.toString());
        if (propiedadId != null) q = q.param("prop", propiedadId.toString());
        return q.query(this::plan).list();
    }

    @Override
    public List<PlanSanitarioItem> items(UUID p, UUID e, boolean all) {
        return jdbc.sql("select * from plan_sanitario_item where plan_id=:p and vigente_hasta is null"
                        + (all ? "" : " and activo") + " order by created_at")
                .param("p", p.toString()).query(this::item).list();
    }

    @Override
    public Optional<PlanSanitarioItem> item(UUID id, UUID e) {
        return jdbc.sql("select * from plan_sanitario_item where id=:id").param("id", id.toString()).query(this::item).optional();
    }

    @Override
    public List<PlanSanitarioItem> versiones(UUID identidadLogicaId, UUID e) {
        return jdbc.sql("select * from plan_sanitario_item where identidad_logica_id=:i order by numero_version desc")
                .param("i", identidadLogicaId.toString()).query(this::item).list();
    }

    @Override
    public PlanSanitarioItem crearItem(PlanSanitarioItem v, UUID a) {
        jdbc.sql("""
                insert into plan_sanitario_item(
                    id, plan_id, identidad_logica_id, numero_version, version_anterior_id, vigente_desde,
                    vigente_hasta, motivo_version, codigo_interno, nombre, descripcion, tipo_actividad,
                    modalidad, modalidad_config, producto_id, producto_recomendado_texto, principio_activo,
                    instrucciones_veterinario, observaciones, dosis, unidad_dosis, dosis_cantidad, dosis_unidad,
                    dosis_unidad_detalle, dosis_tipo_calculo, dosis_peso_referencia_kg, dosis_minima, dosis_maxima,
                    via_administracion, via_administracion_codigo, via_administracion_detalle, lugar_aplicacion,
                    lugar_aplicacion_detalle, categoria_animal_id, categorias_aplicables, sexo_aplicable,
                    edad_min_dias, edad_max_dias, edad_unidad, permite_edad_desconocida, frecuencia_dias,
                    dias_alerta, obligatorio, origen_regulatorio, especie_aplicable, requiere_revision, activo,
                    created_by, updated_by)
                values(
                    :id, :p, :identidad, :numVersion, :versionAnterior, :vigenteDesde, :vigenteHasta, :motivoVersion,
                    :codigoInterno, :nombre, :descripcion, :tipo, :modalidad, :modalidadConfig, :prod, :texto,
                    :principioActivo, :instrucciones, :observaciones, :dosis, :unidad, :dosisCantidad, :dosisUnidad,
                    :dosisUnidadDetalle, :dosisTipoCalculo, :dosisPesoRef, :dosisMin, :dosisMax, :via, :viaCodigo,
                    :viaDetalle, :lugar, :lugarDetalle, :cat, :categoriasAplicables, :sexo, :emin, :emax, :edadUnidad,
                    :permiteDesconocida, :freq, :alerta, :obl, :origen, :especie, :requiereRevision, :activo, :a, :a)
                """)
                .params(params(v, a)).update();
        return item(v.id(), v.empresaId()).orElseThrow();
    }

    @Override
    public PlanSanitarioItem actualizarItem(PlanSanitarioItem v, UUID a) {
        Map<String, Object> p = params(v, a);
        p.put("v", v.version());
        int n = jdbc.sql("""
                update plan_sanitario_item set
                    codigo_interno=:codigoInterno, nombre=:nombre, descripcion=:descripcion, tipo_actividad=:tipo,
                    modalidad=:modalidad, modalidad_config=:modalidadConfig, producto_id=:prod,
                    producto_recomendado_texto=:texto, principio_activo=:principioActivo,
                    instrucciones_veterinario=:instrucciones, observaciones=:observaciones, dosis=:dosis,
                    unidad_dosis=:unidad, dosis_cantidad=:dosisCantidad, dosis_unidad=:dosisUnidad,
                    dosis_unidad_detalle=:dosisUnidadDetalle, dosis_tipo_calculo=:dosisTipoCalculo,
                    dosis_peso_referencia_kg=:dosisPesoRef, dosis_minima=:dosisMin, dosis_maxima=:dosisMax,
                    via_administracion=:via, via_administracion_codigo=:viaCodigo,
                    via_administracion_detalle=:viaDetalle, lugar_aplicacion=:lugar,
                    lugar_aplicacion_detalle=:lugarDetalle, categoria_animal_id=:cat,
                    categorias_aplicables=:categoriasAplicables, sexo_aplicable=:sexo, edad_min_dias=:emin,
                    edad_max_dias=:emax, edad_unidad=:edadUnidad, permite_edad_desconocida=:permiteDesconocida,
                    frecuencia_dias=:freq, dias_alerta=:alerta, obligatorio=:obl, origen_regulatorio=:origen,
                    especie_aplicable=:especie, requiere_revision=:requiereRevision, activo=:activo,
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), updated_by=:a, version=version+1
                where id=:id and version=:v
                """)
                .params(p).update();
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return item(v.id(), v.empresaId()).orElseThrow();
    }

    @Override
    public void cerrarVigenciaItem(UUID id, Instant vigenteHasta, long version, UUID actor) {
        int n = jdbc.sql("""
                update plan_sanitario_item set vigente_hasta=:vh,
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), updated_by=:a, version=version+1
                where id=:id and version=:v
                """)
                .param("vh", vigenteHasta.toString()).param("a", actor.toString())
                .param("id", id.toString()).param("v", version).update();
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
    }

    @Override
    public PlanSanitarioItem cambiarEstadoItem(UUID id, UUID p, UUID e, boolean activo, long version, UUID actor) {
        int n = jdbc.sql("""
                update plan_sanitario_item set activo=:activo,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:a,version=version+1
                where id=:id and plan_id=:p and version=:v
                """)
                .param("activo", activo).param("a", actor.toString()).param("id", id.toString())
                .param("p", p.toString()).param("v", version).update();
        if (n == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return item(id, e).orElseThrow();
    }

    @Override
    public boolean itemEnUso(UUID id) {
        Boolean enEvento = jdbc.sql("select exists(select 1 from evento_calendario_sanitario where actividad_id=:id)")
                .param("id", id.toString()).query(Boolean.class).single();
        Boolean enAplicacion = jdbc.sql("select exists(select 1 from aplicacion_sanitaria where plan_item_id=:id)")
                .param("id", id.toString()).query(Boolean.class).single();
        return Boolean.TRUE.equals(enEvento) || Boolean.TRUE.equals(enAplicacion);
    }

    private Map<String, Object> params(PlanSanitarioItem v, UUID a) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", v.id().toString());
        p.put("p", v.planId().toString());
        p.put("identidad", v.identidadLogicaId().toString());
        p.put("numVersion", v.numeroVersion());
        p.put("versionAnterior", v.versionAnteriorId() == null ? null : v.versionAnteriorId().toString());
        p.put("vigenteDesde", v.vigenteDesde().toString());
        p.put("vigenteHasta", v.vigenteHasta() == null ? null : v.vigenteHasta().toString());
        p.put("motivoVersion", v.motivoVersion());
        p.put("codigoInterno", v.codigoInterno());
        p.put("nombre", v.nombre());
        p.put("descripcion", v.descripcion());
        p.put("tipo", v.tipoActividad().name());
        p.put("modalidad", v.modalidad().name());
        p.put("modalidadConfig", json.writeValueAsString(v.modalidadConfig()));
        p.put("prod", v.productoId() == null ? null : v.productoId().toString());
        p.put("texto", v.productoRecomendadoTexto());
        p.put("principioActivo", v.principioActivo());
        p.put("instrucciones", v.instruccionesVeterinario());
        p.put("observaciones", v.observaciones());
        p.put("dosis", v.dosis());
        p.put("unidad", v.unidadDosis());
        p.put("dosisCantidad", v.dosisCantidad());
        p.put("dosisUnidad", v.dosisUnidad() == null ? null : v.dosisUnidad().name());
        p.put("dosisUnidadDetalle", v.dosisUnidadDetalle());
        p.put("dosisTipoCalculo", v.dosisTipoCalculo().name());
        p.put("dosisPesoRef", v.dosisPesoReferenciaKg());
        p.put("dosisMin", v.dosisMinima());
        p.put("dosisMax", v.dosisMaxima());
        p.put("via", v.viaAdministracion());
        p.put("viaCodigo", v.viaAdministracionCodigo() == null ? null : v.viaAdministracionCodigo().name());
        p.put("viaDetalle", v.viaAdministracionDetalle());
        p.put("lugar", v.lugarAplicacion() == null ? null : v.lugarAplicacion().name());
        p.put("lugarDetalle", v.lugarAplicacionDetalle());
        p.put("cat", v.categoriaAnimalId() == null ? null : v.categoriaAnimalId().toString());
        p.put("categoriasAplicables", json.writeValueAsString(
                v.categoriasAplicables() == null ? List.of() : v.categoriasAplicables()));
        p.put("sexo", v.sexoAplicable() == null ? null : v.sexoAplicable().name());
        p.put("emin", v.edadMinDias());
        p.put("emax", v.edadMaxDias());
        p.put("edadUnidad", v.edadUnidad().name());
        p.put("permiteDesconocida", v.permiteEdadDesconocida());
        p.put("freq", v.frecuenciaDias());
        p.put("alerta", v.diasAlerta());
        p.put("obl", v.obligatorio());
        p.put("origen", v.origenRegulatorio().name());
        p.put("especie", v.especieAplicable());
        p.put("requiereRevision", v.requiereRevision());
        p.put("activo", v.activo());
        p.put("a", a.toString());
        return p;
    }

    private Enfermedad enfermedad(ResultSet r, int n) throws SQLException {
        return new Enfermedad(Rows.uuid(r, "id"), null, r.getString("codigo"), r.getString("nombre"),
                r.getString("descripcion"), r.getBoolean("es_notificable"), r.getBoolean("activo"),
                Rows.instant(r, "created_at"), Rows.instant(r, "updated_at"));
    }

    private PlanSanitario plan(ResultSet r, int n) throws SQLException {
        return new PlanSanitario(Rows.uuid(r, "id"), null, r.getString("nombre"), r.getString("descripcion"),
                Rows.localDate(r, "fecha_inicio"), Rows.localDate(r, "fecha_fin"),
                EstadoPlanSanitario.valueOf(r.getString("estado")), Rows.instant(r, "created_at"),
                Rows.instant(r, "updated_at"), r.getLong("version"), Rows.uuid(r, "propiedad_id"));
    }

    private PlanSanitarioItem item(ResultSet r, int n) throws SQLException {
        String sexo = r.getString("sexo_aplicable");
        String dosisUnidad = r.getString("dosis_unidad");
        String viaCodigo = r.getString("via_administracion_codigo");
        String lugar = r.getString("lugar_aplicacion");
        List<UUID> categorias = json.readValue(
                r.getString("categorias_aplicables") == null ? "[]" : r.getString("categorias_aplicables"),
                new TypeReference<>() {});
        return new PlanSanitarioItem(
                Rows.uuid(r, "id"), null, Rows.uuid(r, "plan_id"), TipoActividadSanitaria.valueOf(r.getString("tipo_actividad")),
                Rows.uuid(r, "producto_id"), r.getString("producto_recomendado_texto"), Rows.uuid(r, "categoria_animal_id"),
                sexo == null ? null : SexoAnimal.valueOf(sexo), Rows.intOrNull(r, "edad_min_dias"),
                Rows.intOrNull(r, "edad_max_dias"), r.getBigDecimal("dosis"), r.getString("unidad_dosis"),
                Rows.intOrNull(r, "frecuencia_dias"), r.getInt("dias_alerta"), r.getString("via_administracion"),
                r.getBoolean("obligatorio"), r.getBoolean("activo"), r.getLong("version"),
                OrigenRegulatorioActividad.valueOf(r.getString("origen_regulatorio")), r.getString("especie_aplicable"),
                r.getBoolean("permite_edad_desconocida"),
                Rows.uuid(r, "identidad_logica_id"), r.getInt("numero_version"), Rows.uuid(r, "version_anterior_id"),
                Rows.instant(r, "vigente_desde"), Rows.instant(r, "vigente_hasta"), r.getString("motivo_version"),
                r.getString("codigo_interno"), r.getString("nombre"), r.getString("descripcion"),
                r.getString("principio_activo"), r.getString("instrucciones_veterinario"), r.getString("observaciones"),
                r.getBigDecimal("dosis_cantidad"), dosisUnidad == null ? null : UnidadDosis.valueOf(dosisUnidad),
                r.getString("dosis_unidad_detalle"), TipoCalculoDosis.valueOf(r.getString("dosis_tipo_calculo")),
                r.getBigDecimal("dosis_peso_referencia_kg"), r.getBigDecimal("dosis_minima"), r.getBigDecimal("dosis_maxima"),
                viaCodigo == null ? null : ViaAdministracion.valueOf(viaCodigo), r.getString("via_administracion_detalle"),
                lugar == null ? null : LugarAplicacion.valueOf(lugar), r.getString("lugar_aplicacion_detalle"),
                categorias, UnidadEdadActividad.valueOf(r.getString("edad_unidad")),
                ModalidadActividad.valueOf(r.getString("modalidad")),
                deserializarModalidadConfig(ModalidadActividad.valueOf(r.getString("modalidad")), r.getString("modalidad_config")),
                r.getBoolean("requiere_revision"));
    }

    private ModalidadConfig deserializarModalidadConfig(ModalidadActividad modalidad, String raw) {
        String value = raw == null ? "{}" : raw;
        return switch (modalidad) {
            case POR_EDAD -> json.readValue(value, ModalidadConfig.PorEdadConfig.class);
            case PERIODICA -> json.readValue(value, ModalidadConfig.PeriodicaConfig.class);
            case FECHA_PROGRAMADA -> json.readValue(value, ModalidadConfig.FechaProgramadaConfig.class);
            case POR_HALLAZGO -> json.readValue(value, ModalidadConfig.PorHallazgoConfig.class);
            case MANUAL -> new ModalidadConfig.ManualConfig();
        };
    }
}
