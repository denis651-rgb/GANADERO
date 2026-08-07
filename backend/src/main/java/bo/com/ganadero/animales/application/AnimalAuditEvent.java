package bo.com.ganadero.animales.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AnimalAuditEvent(
        UUID empresaId,
        UUID usuarioId,
        String accion,
        String entidad,
        UUID entidadId,
        Instant occurredAt,
        Map<String, Object> datos) {

    public AnimalAuditEvent(UUID empresaId, UUID usuarioId, String accion, String entidad, UUID entidadId, Instant occurredAt) {
        this(empresaId, usuarioId, accion, entidad, entidadId, occurredAt, Map.of());
    }
}
