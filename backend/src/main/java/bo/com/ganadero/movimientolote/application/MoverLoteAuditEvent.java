package bo.com.ganadero.movimientolote.application;

import java.time.Instant;
import java.util.UUID;

public record MoverLoteAuditEvent(UUID empresaId, UUID usuarioId, String accion, UUID entidadId, Instant occurredAt) {
}
