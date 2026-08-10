package bo.com.ganadero.shared.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SyncAuditEvent(
        UUID empresaId,
        UUID usuarioId,
        String accion,
        String entidad,
        UUID entidadId,
        Map<String, Object> datos,
        Instant occurredAt) {
}
