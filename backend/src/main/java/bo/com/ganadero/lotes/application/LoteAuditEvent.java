package bo.com.ganadero.lotes.application;

import java.time.Instant;
import java.util.UUID;

public record LoteAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad, UUID entidadId, Instant occurredAt) {
}
