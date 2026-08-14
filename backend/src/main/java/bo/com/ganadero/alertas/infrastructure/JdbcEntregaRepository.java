package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.domain.EntregaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class JdbcEntregaRepository implements EntregaRepository {
    private final JdbcClient jdbc;

    public JdbcEntregaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void registrarPendiente(UUID alertaId, UUID suscripcionId) {
        jdbc.sql("insert into alertas.entregas_notificacion(id,alerta_id,suscripcion_id,estado,intentos) "
                + "values(:id,:a,:s,'PENDIENTE',0) "
                + "on conflict(alerta_id,suscripcion_id) do update set estado='PENDIENTE',updated_at=now()")
                .param("id", UUID.randomUUID())
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    public void marcarEnviada(UUID alertaId, UUID suscripcionId, Instant enviadaAt) {
        jdbc.sql("update alertas.entregas_notificacion set estado='ENVIADA',enviada_at=:t,intentos=intentos+1,"
                + "ultimo_error=null,updated_at=now() where alerta_id=:a and suscripcion_id=:s")
                .param("t", Timestamp.from(enviadaAt))
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    public void marcarError(UUID alertaId, UUID suscripcionId, String error) {
        jdbc.sql("update alertas.entregas_notificacion set estado='ERROR',intentos=intentos+1,"
                + "ultimo_error=:e,updated_at=now() where alerta_id=:a and suscripcion_id=:s")
                .param("e", truncate(error))
                .param("a", alertaId)
                .param("s", suscripcionId)
                .update();
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }
}
