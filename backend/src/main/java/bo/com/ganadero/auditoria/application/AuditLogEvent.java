package bo.com.ganadero.auditoria.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogEvent(
        UUID empresaId,
        UUID usuarioId,
        String accion,
        String modulo,
        String entidad,
        UUID entidadId,
        Map<String, Object> datosAnteriores,
        Map<String, Object> datosNuevos,
        Instant occurredAt) {
}
