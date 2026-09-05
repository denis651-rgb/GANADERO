package bo.com.ganadero.compras.application;

import java.time.Instant;
import java.util.UUID;

public record CompraAuditEvent(UUID empresaId, UUID usuarioId, String accion, UUID entidadId, Instant occurredAt) {
}
