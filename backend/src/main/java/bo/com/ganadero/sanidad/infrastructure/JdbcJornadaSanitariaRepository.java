package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.pesajes.domain.TipoPeso;
import bo.com.ganadero.sanidad.domain.AplicacionSanitaria;
import bo.com.ganadero.sanidad.domain.EstadoAplicacionSanitaria;
import bo.com.ganadero.sanidad.domain.EstadoJornada;
import bo.com.ganadero.sanidad.domain.JornadaSanitaria;
import bo.com.ganadero.sanidad.domain.JornadaSanitariaRepository;
import bo.com.ganadero.sanidad.domain.LugarAplicacion;
import bo.com.ganadero.sanidad.domain.OrigenRegistroAplicacion;
import bo.com.ganadero.sanidad.domain.PlanSanitarioItem;
import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcJornadaSanitariaRepository implements JornadaSanitariaRepository {

    private final JdbcClient jdbc;

    public JdbcJornadaSanitariaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public JornadaSanitaria crear(JornadaSanitaria j, UUID actor) {
        String sql = """
                INSERT INTO jornada_sanitaria(id, tipo_jornada, fecha_inicio, fecha_fin, propiedad_id,
                                              potrero_id, lote_ganadero_id, responsable_id, veterinario_id,
                                              estado, observaciones, operation_id, created_by, updated_by)
                VALUES(:id, :t, :ini, :fin, :prop, :pot, :lote, :r, :v, :estado, :o, :op, :a, :a)
                """;
        jdbc.sql(sql)
                .params(params(j, actor))
                .update();
        return buscar(j.id(), j.empresaId()).orElseThrow();
    }

    @Override
    public JornadaSanitaria actualizar(JornadaSanitaria j, UUID actor) {
        String sql = """
                UPDATE jornada_sanitaria
                SET tipo_jornada = :t,
                    fecha_inicio = :ini,
                    propiedad_id = :prop,
                    potrero_id = :pot,
                    lote_ganadero_id = :lote,
                    responsable_id = :r,
                    veterinario_id = :v,
                    observaciones = :o,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                    updated_by = :a,
                    version = version + 1
                WHERE id = :id AND estado = 'BORRADOR' AND version = :version
                """;
        int n = jdbc.sql(sql)
                .params(params(j, actor))
                .param("version", j.version())
                .update();
        if (n == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        return buscar(j.id(), j.empresaId()).orElseThrow();
    }

    @Override
    public JornadaSanitaria anular(UUID id, UUID empresaId, long version, String motivo, UUID actor) {
        String sql = """
                UPDATE jornada_sanitaria
                SET estado = 'ANULADA',
                    observaciones = CASE
                        WHEN observaciones IS NULL OR trim(observaciones) = '' THEN 'Anulación: ' || :motivo
                        ELSE observaciones || char(10) || 'Anulación: ' || :motivo
                    END,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                    updated_by = :a,
                    version = version + 1
                WHERE id = :id AND estado = 'BORRADOR' AND version = :version
                """;
        int n = jdbc.sql(sql)
                .param("motivo", motivo.trim())
                .param("a", actor.toString())
                .param("id", id.toString())
                .param("version", version)
                .update();
        if (n == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        return buscar(id, empresaId).orElseThrow();
    }

    @Override
    public Optional<JornadaSanitaria> buscar(UUID id, UUID empresaId) {
        return jdbc.sql("SELECT * FROM jornada_sanitaria WHERE id = :id")
                .param("id", id.toString())
                .query(this::mapJ)
                .optional();
    }

    @Override
    public Optional<JornadaSanitaria> buscarPorOperacion(UUID op, UUID empresaId) {
        return jdbc.sql("SELECT * FROM jornada_sanitaria WHERE operation_id = :op")
                .param("op", op.toString())
                .query(this::mapJ)
                .optional();
    }

    @Override
    public List<JornadaSanitaria> listar(UUID empresaId) {
        return jdbc.sql("SELECT * FROM jornada_sanitaria ORDER BY fecha_inicio DESC")
                .query(this::mapJ)
                .list();
    }

    @Override
    public void reemplazarSeleccion(UUID jornadaId, UUID empresaId, Collection<UUID> animales) {
        jdbc.sql("DELETE FROM jornada_animal WHERE jornada_id = :j")
                .param("j", jornadaId.toString())
                .update();
        for (UUID id : animales) {
            jdbc.sql("INSERT INTO jornada_animal(jornada_id, animal_id) VALUES(:j, :a) ON CONFLICT DO NOTHING")
                    .param("j", jornadaId.toString())
                    .param("a", id.toString())
                    .update();
        }
    }

    @Override
    public List<UUID> seleccion(UUID jornadaId, UUID empresaId) {
        return jdbc.sql("SELECT animal_id FROM jornada_animal WHERE jornada_id = :j ORDER BY animal_id")
                .param("j", jornadaId.toString())
                .query(String.class)
                .list()
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    @Override
    public JornadaSanitaria iniciarConfirmacion(UUID id, UUID empresaId, long version, UUID operationId, UUID actor) {
        String sql = """
                UPDATE jornada_sanitaria
                SET estado = 'EN_PROCESO',
                    operation_id = :op,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                    updated_by = :a,
                    version = version + 1
                WHERE id = :id AND estado = 'BORRADOR' AND version = :v
                """;
        int n = jdbc.sql(sql)
                .param("op", operationId.toString())
                .param("a", actor.toString())
                .param("id", id.toString())
                .param("v", version)
                .update();
        if (n == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        return buscar(id, empresaId).orElseThrow();
    }

    @Override
    public JornadaSanitaria confirmar(UUID id, UUID empresaId, UUID actor) {
        String sql = """
                UPDATE jornada_sanitaria
                SET estado = 'CONFIRMADA',
                    fecha_fin = COALESCE(fecha_fin, date('now')),
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                    updated_by = :a,
                    version = version + 1
                WHERE id = :id AND estado = 'EN_PROCESO'
                """;
        jdbc.sql(sql)
                .param("a", actor.toString())
                .param("id", id.toString())
                .update();
        return buscar(id, empresaId).orElseThrow();
    }

    @Override
    public AplicacionSanitaria crearAplicacion(AplicacionSanitaria x, UUID actor) {
        String sql = """
                INSERT INTO aplicacion_sanitaria(id, jornada_id, plan_item_id, animal_id, dosis, unidad_dosis,
                                                 dosis_recomendada, dosis_aplicada, peso_utilizado_kg, peso_tipo,
                                                 peso_fecha, producto_aplicado_texto, motivo_cambio_producto,
                                                 motivo_ajuste_dosis, via_administracion, lugar_aplicacion,
                                                 version_actividad_id, instrucciones_aplicadas_texto,
                                                 evento_calendario_id, fecha_aplicacion, proxima_aplicacion,
                                                 retiro_carne_hasta, retiro_leche_hasta, aplicado_por, resultado,
                                                 observaciones, idempotency_key, estado, origen_registro, created_by)
                VALUES(:id, :j, :pi, :animal, :d, :u, :dr, :da, :pk, :pt, :pf, :prodTexto, :motivoProd,
                       :motivoDosis, :via, :lugar, :versionAct, :instrucciones, :evento, :f, :prox, :rc, :rl,
                       :por, :res, :o, :key, :estado, :origen, :a)
                ON CONFLICT(idempotency_key) DO NOTHING
                """;
        jdbc.sql(sql)
                .params(params(x, actor))
                .update();
        return aplicacion(x.id(), x.empresaId()).orElseThrow();
    }

    @Override
    public Optional<AplicacionSanitaria> aplicacion(UUID id, UUID empresaId) {
        return jdbc.sql("SELECT * FROM aplicacion_sanitaria WHERE id = :id")
                .param("id", id.toString())
                .query(this::mapA)
                .optional();
    }

    @Override
    public List<AplicacionSanitaria> aplicaciones(UUID jornadaId, UUID empresaId) {
        return jdbc.sql("SELECT * FROM aplicacion_sanitaria WHERE jornada_id = :j ORDER BY created_at")
                .param("j", jornadaId.toString())
                .query(this::mapA)
                .list();
    }

    @Override
    public List<UUID> aplicacionesPrevias(UUID empresaId, UUID animalId, UUID planItemId, UUID excluirId) {
        String sql = """
                SELECT id FROM aplicacion_sanitaria
                WHERE animal_id = :a AND plan_item_id = :i AND id <> :x AND estado = 'APLICADO'
                """;
        return jdbc.sql(sql)
                .param("a", animalId.toString())
                .param("i", planItemId.toString())
                .param("x", excluirId.toString())
                .query(String.class)
                .list()
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    @Override
    public List<AplicacionSanitaria> vacunacionesRelacionadas(UUID empresaId, UUID animalId, PlanSanitarioItem item, UUID productoId) {
        String sql = """
                SELECT a.* FROM aplicacion_sanitaria a
                LEFT JOIN plan_sanitario_item i ON i.id = a.plan_item_id
                WHERE a.animal_id = :animal AND a.estado = 'APLICADO'
                  AND (a.plan_item_id = :item
                    OR (:texto IS NOT NULL AND lower(trim(coalesce(a.producto_aplicado_texto, i.producto_recomendado_texto))) = :texto))
                """;
        String texto = item.productoRecomendadoTexto() == null || item.productoRecomendadoTexto().isBlank()
                ? null : item.productoRecomendadoTexto().trim().toLowerCase(Locale.ROOT);

        return jdbc.sql(sql)
                .param("animal", animalId.toString())
                .param("item", item.id().toString())
                .param("texto", texto)
                .query(this::mapA)
                .list();
    }

    private Map<String, Object> params(JornadaSanitaria j, UUID actor) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", j.id().toString());
        p.put("t", j.tipoJornada().name());
        p.put("ini", j.fechaInicio() == null ? null : j.fechaInicio().toString());
        p.put("fin", j.fechaFin() == null ? null : j.fechaFin().toString());
        p.put("prop", j.propiedadId() == null ? null : j.propiedadId().toString());
        p.put("pot", j.potreroId() == null ? null : j.potreroId().toString());
        p.put("lote", j.loteGanaderoId() == null ? null : j.loteGanaderoId().toString());
        p.put("r", j.responsableId() == null ? null : j.responsableId().toString());
        p.put("v", j.veterinarioId() == null ? null : j.veterinarioId().toString());
        p.put("estado", j.estado().name());
        p.put("o", j.observaciones());
        p.put("op", j.operationId() == null ? null : j.operationId().toString());
        p.put("a", actor.toString());
        return p;
    }

    private Map<String, Object> params(AplicacionSanitaria x, UUID actor) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", x.id().toString());
        p.put("j", x.jornadaId() == null ? null : x.jornadaId().toString());
        p.put("pi", x.planItemId() == null ? null : x.planItemId().toString());
        p.put("animal", x.animalId().toString());
        p.put("d", x.dosis());
        p.put("u", x.unidadDosis());
        p.put("dr", x.dosisRecomendada());
        p.put("da", x.dosisAplicada());
        p.put("pk", x.pesoUtilizadoKg());
        p.put("pt", x.pesoTipo() == null ? null : x.pesoTipo().name());
        p.put("pf", x.pesoFecha() == null ? null : x.pesoFecha().toString());
        p.put("prodTexto", x.productoAplicadoTexto());
        p.put("motivoProd", x.motivoCambioProducto());
        p.put("motivoDosis", x.motivoAjusteDosis());
        p.put("via", x.viaAdministracion());
        p.put("lugar", x.lugarAplicacion() == null ? null : x.lugarAplicacion().name());
        p.put("versionAct", x.versionActividadId() == null ? null : x.versionActividadId().toString());
        p.put("instrucciones", x.instruccionesAplicadasTexto());
        p.put("evento", x.eventoCalendarioId() == null ? null : x.eventoCalendarioId().toString());
        p.put("f", x.fechaAplicacion() == null ? null : x.fechaAplicacion().toString());
        p.put("prox", x.proximaAplicacion() == null ? null : x.proximaAplicacion().toString());
        p.put("rc", x.retiroCarneHasta() == null ? null : x.retiroCarneHasta().toString());
        p.put("rl", x.retiroLecheHasta() == null ? null : x.retiroLecheHasta().toString());
        p.put("por", x.aplicadoPor() == null ? null : x.aplicadoPor().toString());
        p.put("res", x.resultado());
        p.put("o", x.observaciones());
        p.put("key", x.idempotencyKey());
        p.put("estado", x.estado().name());
        p.put("origen", x.origenRegistro().name());
        p.put("a", actor.toString());
        return p;
    }

    private JornadaSanitaria mapJ(ResultSet r, int n) throws SQLException {
        return new JornadaSanitaria(
                Rows.uuid(r, "id"),
                null,
                TipoActividadSanitaria.valueOf(r.getString("tipo_jornada")),
                r.getString("fecha_inicio") == null ? null : LocalDate.parse(r.getString("fecha_inicio")),
                r.getString("fecha_fin") == null ? null : LocalDate.parse(r.getString("fecha_fin")),
                Rows.uuid(r, "propiedad_id"),
                Rows.uuid(r, "potrero_id"),
                Rows.uuid(r, "lote_ganadero_id"),
                Rows.uuid(r, "responsable_id"),
                Rows.uuid(r, "veterinario_id"),
                EstadoJornada.valueOf(r.getString("estado")),
                r.getString("observaciones"),
                Rows.uuid(r, "operation_id"),
                r.getLong("version")
        );
    }

    private AplicacionSanitaria mapA(ResultSet r, int n) throws SQLException {
        String pesoTipo = r.getString("peso_tipo");
        String lugar = r.getString("lugar_aplicacion");
        return new AplicacionSanitaria(
                Rows.uuid(r, "id"),
                null,
                Rows.uuid(r, "jornada_id"),
                Rows.uuid(r, "plan_item_id"),
                Rows.uuid(r, "animal_id"),
                Rows.uuid(r, "producto_id"),
                Rows.uuid(r, "lote_producto_id"),
                r.getBigDecimal("dosis"),
                r.getString("unidad_dosis"),
                r.getBigDecimal("dosis_recomendada"),
                r.getBigDecimal("dosis_aplicada"),
                r.getBigDecimal("peso_utilizado_kg"),
                pesoTipo == null ? null : TipoPeso.valueOf(pesoTipo),
                Rows.instant(r, "peso_fecha"),
                r.getString("producto_aplicado_texto"),
                r.getString("motivo_cambio_producto"),
                r.getString("motivo_ajuste_dosis"),
                r.getString("via_administracion"),
                lugar == null ? null : LugarAplicacion.valueOf(lugar),
                Rows.uuid(r, "version_actividad_id"),
                r.getString("instrucciones_aplicadas_texto"),
                Rows.uuid(r, "evento_calendario_id"),
                Rows.localDate(r, "fecha_aplicacion"),
                Rows.localDate(r, "proxima_aplicacion"),
                Rows.localDate(r, "retiro_carne_hasta"),
                Rows.localDate(r, "retiro_leche_hasta"),
                Rows.uuid(r, "aplicado_por"),
                r.getString("resultado"),
                r.getString("observaciones"),
                r.getString("idempotency_key"),
                EstadoAplicacionSanitaria.valueOf(r.getString("estado")),
                r.getLong("version"),
                OrigenRegistroAplicacion.valueOf(r.getString("origen_registro"))
        );
    }
}

