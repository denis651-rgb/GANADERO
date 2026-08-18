package bo.com.ganadero.alertas.api;

import bo.com.ganadero.alertas.domain.SuscripcionPush;

import java.time.Instant;
import java.util.UUID;

public record PushDeviceResponse(
        UUID id,
        String endpoint,
        String dispositivoNombre,
        String userAgent,
        Instant ultimoUsoAt
) {
    public static PushDeviceResponse from(SuscripcionPush subscription) {
        return new PushDeviceResponse(subscription.id(), subscription.endpoint(),
                subscription.dispositivoNombre(), subscription.userAgent(), subscription.ultimoUsoAt());
    }
}
