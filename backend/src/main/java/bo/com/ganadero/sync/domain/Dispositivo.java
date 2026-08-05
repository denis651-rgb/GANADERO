package bo.com.ganadero.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record Dispositivo(
        UUID id,
        UUID empresaId,
        UUID usuarioId,
        String codigoDispositivo,
        String nombre,
        String plataforma,
        String versionApp,
        EstadoDispositivo estado,
        Instant ultimoSeenAt,
        long ultimoCursor,
        long version) {
}
