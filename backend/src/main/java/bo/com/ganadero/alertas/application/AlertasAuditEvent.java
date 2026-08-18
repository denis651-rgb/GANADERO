package bo.com.ganadero.alertas.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AlertasAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad,
                                UUID entidadId, Map<String, Object> datos, Instant occurredAt) {}
