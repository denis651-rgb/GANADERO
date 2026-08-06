package bo.com.ganadero.movimientos.application;

import java.time.Instant;
import java.util.UUID;

public record MovimientoAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad, UUID entidadId, Instant occurredAt) {
}
