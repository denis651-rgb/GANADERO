package bo.com.ganadero.sync.api;

import java.time.Instant;
import java.util.UUID;

public record CambioResponse(
        long id,
        String tabla,
        UUID entidadId,
        String tipoCambio,
        Object datos,
        String dispositivoOrigen,
        Instant createdAt) {
}
