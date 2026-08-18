package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.EntregaPendiente;
import bo.com.ganadero.alertas.domain.EntregaRepository;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcEntregaRepository implements EntregaRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcEntregaRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void registrarPendiente(UUID alertaId, UUID suscripcionId) {
        jdbc.sql("insert into alertas.entregas_notificacion(id,alerta_id,suscripcion_id,estado,intentos) "
                + "values(:id,:a,:s,'PENDIENTE',0) "
                + "on conflict(alerta_id,suscripcion_id) do nothing")
                .param("id", UUID.randomUUID())
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    public List<EntregaPendiente> listarPendientes(int maxIntentos, int limite) {
        return jdbc.sql("""
                select e.id as e_id, e.intentos as e_intentos,
                       a.id as a_id, a.empresa_id as a_empresa_id, a.animal_id as a_animal_id,
                       a.tipo as a_tipo, a.titulo as a_titulo, a.mensaje as a_mensaje,
                       a.severidad as a_severidad, a.fecha_programada as a_fecha_programada,
                       a.fecha_vencimiento as a_fecha_vencimiento, a.origen_tipo as a_origen_tipo,
                       a.origen_id as a_origen_id, a.estado as a_estado, a.metadata as a_metadata,
                       a.enviada_at as a_enviada_at, a.atendida_at as a_atendida_at,
                       a.resuelta_at as a_resuelta_at, a.cancelada_at as a_cancelada_at,
                       a.atendida_por as a_atendida_por, a.resuelta_por as a_resuelta_por,
                       a.motivo_cancelacion as a_motivo_cancelacion, a.intentos_envio as a_intentos_envio,
                       a.ultimo_error as a_ultimo_error, a.created_at as a_created_at,
                       a.updated_at as a_updated_at, a.clave_idempotencia as a_clave_idempotencia,
                       s.id as s_id, s.empresa_id as s_empresa_id, s.usuario_id as s_usuario_id,
                       s.endpoint as s_endpoint, s.p256dh as s_p256dh, s.auth as s_auth,
                       s.dispositivo_nombre as s_dispositivo_nombre, s.user_agent as s_user_agent,
                       s.activo as s_activo, s.created_at as s_created_at, s.updated_at as s_updated_at,
                       s.ultimo_uso_at as s_ultimo_uso_at
                from alertas.entregas_notificacion e
                join alertas.alertas a on a.id = e.alerta_id
                join alertas.suscripciones_push s on s.id = e.suscripcion_id
                where e.estado = 'PENDIENTE'
                   or (e.estado = 'ERROR' and e.intentos < :m
                       and coalesce(e.proximo_intento_at, e.updated_at) <= now())
                order by e.created_at
                limit :l
                """)
                .param("m", maxIntentos)
                .param("l", limite)
                .query(this::mapPendiente)
                .list();
    }

    public void marcarEnviada(UUID alertaId, UUID suscripcionId, Instant enviadaAt) {
        jdbc.sql("update alertas.entregas_notificacion set estado='ENVIADA',enviada_at=:t,intentos=intentos+1,"
                + "ultimo_error=null,updated_at=now() where alerta_id=:a and suscripcion_id=:s")
                .param("t", Timestamp.from(enviadaAt))
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    public void marcarError(UUID alertaId, UUID suscripcionId, String error, Instant proximoIntentoAt) {
        jdbc.sql("update alertas.entregas_notificacion set estado='ERROR',intentos=intentos+1,"
                + "ultimo_error=:e,proximo_intento_at=:p,updated_at=now() where alerta_id=:a and suscripcion_id=:s")
                .param("e", truncate(error))
                .param("p", Timestamp.from(proximoIntentoAt))
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    public void marcarDescartada(UUID alertaId, UUID suscripcionId) {
        jdbc.sql("update alertas.entregas_notificacion set estado='DESCARTADA',proximo_intento_at=null,"
                + "updated_at=now() where alerta_id=:a and suscripcion_id=:s")
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    public boolean tienePendientes(UUID alertaId, int maxIntentos) {
        Integer count = jdbc.sql("select count(*) from alertas.entregas_notificacion "
                + "where alerta_id=:a and (estado='PENDIENTE' or (estado='ERROR' and intentos<:m))")
                .param("a", alertaId)
                .param("m", maxIntentos)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public boolean tieneEnviadas(UUID alertaId) {
        Integer count = jdbc.sql("select count(*) from alertas.entregas_notificacion where alerta_id=:a and estado='ENVIADA'")
                .param("a", alertaId).query(Integer.class).single();
        return count != null && count > 0;
    }

    public String resumenFallos(UUID alertaId) {
        return jdbc.sql("select coalesce(string_agg(distinct ultimo_error, '; '), 'No se pudo entregar la notificación') "
                        + "from alertas.entregas_notificacion where alerta_id=:a and estado in('ERROR','DESCARTADA')")
                .param("a", alertaId).query(String.class).single();
    }

    private EntregaPendiente mapPendiente(ResultSet r, int row) throws SQLException {
        Alerta alerta = new Alerta(
                r.getObject("a_id", UUID.class), r.getObject("a_empresa_id", UUID.class),
                r.getObject("a_animal_id", UUID.class), TipoAlerta.valueOf(r.getString("a_tipo")),
                r.getString("a_titulo"), r.getString("a_mensaje"),
                SeveridadAlerta.valueOf(r.getString("a_severidad")),
                instant(r, "a_fecha_programada"), instant(r, "a_fecha_vencimiento"),
                r.getString("a_origen_tipo"), r.getObject("a_origen_id", UUID.class),
                EstadoAlerta.valueOf(r.getString("a_estado")), read(r.getString("a_metadata")),
                instant(r, "a_enviada_at"), instant(r, "a_atendida_at"),
                instant(r, "a_resuelta_at"), instant(r, "a_cancelada_at"),
                r.getObject("a_atendida_por", UUID.class), r.getObject("a_resuelta_por", UUID.class),
                r.getString("a_motivo_cancelacion"), r.getInt("a_intentos_envio"),
                r.getString("a_ultimo_error"), instant(r, "a_created_at"), instant(r, "a_updated_at"),
                r.getString("a_clave_idempotencia"));
        SuscripcionPush suscripcion = new SuscripcionPush(
                r.getObject("s_id", UUID.class), r.getObject("s_empresa_id", UUID.class),
                r.getObject("s_usuario_id", UUID.class), r.getString("s_endpoint"),
                r.getString("s_p256dh"), r.getString("s_auth"), r.getString("s_dispositivo_nombre"),
                r.getString("s_user_agent"), r.getBoolean("s_activo"),
                instant(r, "s_created_at"), instant(r, "s_updated_at"), instant(r, "s_ultimo_uso_at"));
        return new EntregaPendiente(r.getObject("e_id", UUID.class), alerta, suscripcion, r.getInt("e_intentos"));
    }

    private Map<String, Object> read(String value) {
        try {
            return json.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Instant instant(ResultSet r, String column) throws SQLException {
        OffsetDateTime value = r.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
