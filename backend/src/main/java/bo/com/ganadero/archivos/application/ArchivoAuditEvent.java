package bo.com.ganadero.archivos.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de auditoría del módulo archivos.
 *
 * <p>Es consumido por el módulo de auditoría (AFTER_COMMIT) para registrar
 * las operaciones sobre fotografías y documentos.</p>
 */
public record ArchivoAuditEvent(
        UUID empresaId,
        UUID usuarioId,
        String accion,
        String entidad,
        UUID entidadId,
        Instant occurredAt,
        Map<String, Object> datos) {

    public ArchivoAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad, UUID entidadId, Instant occurredAt) {
        this(empresaId, usuarioId, accion, entidad, entidadId, occurredAt, Map.of());
    }
}
