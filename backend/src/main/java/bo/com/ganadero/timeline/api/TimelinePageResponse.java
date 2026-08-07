package bo.com.ganadero.timeline.api;

import bo.com.ganadero.timeline.domain.EventoTimelinePage;

import java.util.List;

/**
 * Respuesta paginada de la línea de tiempo del animal (Bloque 31).
 */
public record TimelinePageResponse(List<TimelineEventResponse> content, int page, int size,
                                   long totalElements, int totalPages) {

    public static TimelinePageResponse from(EventoTimelinePage p) {
        return new TimelinePageResponse(p.content().stream().map(TimelineEventResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
