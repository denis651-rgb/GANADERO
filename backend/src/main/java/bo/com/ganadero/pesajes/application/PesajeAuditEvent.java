package bo.com.ganadero.pesajes.application;

import java.time.Instant;
import java.util.UUID;

public record PesajeAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad, UUID entidadId, Instant occurredAt) {
}
