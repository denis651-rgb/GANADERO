package bo.com.ganadero.integraciones.telegram;

import java.time.Instant;
import java.util.UUID;

public record ConfiguracionTelegram(
    UUID id,
    UUID empresaId,
    String botToken,
    String chatIdPrincipal,
    String chatIdsJson,
    boolean activo,
    boolean notificarSanidad,
    boolean notificarReproduccion,
    String horaNotificacion,
    Instant createdAt,
    Instant updatedAt,
    int version
) {
    public static ConfiguracionTelegram porDefecto(UUID empresaId) {
        return new ConfiguracionTelegram(
            UUID.randomUUID(),
            empresaId,
            "",
            "",
            "[]",
            true,
            true,
            true,
            "07:00",
            Instant.now(),
            Instant.now(),
            0
        );
    }
}
