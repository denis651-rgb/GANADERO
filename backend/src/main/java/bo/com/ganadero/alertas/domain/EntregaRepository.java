package bo.com.ganadero.alertas.domain;

import java.time.Instant;
import java.util.UUID;

public interface EntregaRepository {
    void registrarPendiente(UUID alertaId, UUID suscripcionId);

    void marcarEnviada(UUID alertaId, UUID suscripcionId, Instant enviadaAt);

    void marcarError(UUID alertaId, UUID suscripcionId, String error);
}
