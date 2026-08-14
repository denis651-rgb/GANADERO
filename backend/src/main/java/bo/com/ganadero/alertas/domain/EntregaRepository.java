package bo.com.ganadero.alertas.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistencia de ENTREGAS: cada intento de comunicar una ALERTA a un
 * dispositivo (suscripción). Una alerta puede tener muchas entregas.
 */
public interface EntregaRepository {
    void registrarPendiente(UUID alertaId, UUID suscripcionId);

    List<EntregaPendiente> listarPendientes(int maxIntentos, int limite);

    void marcarEnviada(UUID alertaId, UUID suscripcionId, Instant enviadaAt);

    void marcarError(UUID alertaId, UUID suscripcionId, String error);

    void marcarDescartada(UUID alertaId, UUID suscripcionId);

    boolean tienePendientes(UUID alertaId, int maxIntentos);
}
