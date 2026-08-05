package bo.com.ganadero.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record CambioSync(
        long id,
        UUID empresaId,
        String tabla,
        UUID entidadId,
        String tipoCambio,
        String datosJson,
        String dispositivoOrigen,
        Instant createdAt) {
}
