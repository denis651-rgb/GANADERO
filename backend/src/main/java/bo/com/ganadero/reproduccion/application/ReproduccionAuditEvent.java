package bo.com.ganadero.reproduccion.application;

import java.time.Instant;
import java.util.UUID;

public record ReproduccionAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad, UUID entidadId, Instant occurredAt) {
}
