package bo.com.ganadero.proveedores.application;

import java.time.Instant;
import java.util.UUID;

public record ProveedorAuditEvent(UUID empresaId, UUID usuarioId, String accion, UUID entidadId, Instant occurredAt) {
}
