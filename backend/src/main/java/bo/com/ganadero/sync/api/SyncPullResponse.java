package bo.com.ganadero.sync.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncPullResponse(
        UUID dispositivoId,
        long cursor,
        boolean hayMas,
        Instant servertime,
        List<CambioResponse> cambios) {
}
