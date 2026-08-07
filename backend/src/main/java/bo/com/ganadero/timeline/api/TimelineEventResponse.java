package bo.com.ganadero.timeline.api;

import bo.com.ganadero.timeline.domain.EventoTimelineAnimal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Respuesta canónica de un evento de la línea de tiempo (Bloque 31).
 */
public record TimelineEventResponse(
        UUID id,
        String tipo,
        String titulo,
        String descripcion,
        Instant fechaTecnica,
        Instant fechaEvento,
        UUID usuarioId,
        String usuarioNombre,
        UUID dispositivoId,
        String moduloOrigen,
        UUID registroOrigenId,
        Map<String, Object> metadata,
        boolean origenSync,
        String idempotencyKey) {

    static TimelineEventResponse from(EventoTimelineAnimal e) {
        boolean origenSync = Boolean.TRUE.equals(e.metadata().get("origenSync"));
        return new TimelineEventResponse(
                e.id(), e.tipo().name(), e.titulo(), e.descripcion(),
                e.fechaTecnica(), e.fechaEvento(), e.usuarioId(), e.usuarioNombre(),
                e.dispositivoId(), e.moduloOrigen(), e.registroOrigenId(),
                e.metadata(), origenSync, e.idempotencyKey());
    }
}
