package bo.com.ganadero.sync.api;

import java.util.List;
import java.util.UUID;

public record SyncPushResponse(
        UUID dispositivoId,
        long nuevoCursor,
        List<OperacionResultado> resultados) {
}
