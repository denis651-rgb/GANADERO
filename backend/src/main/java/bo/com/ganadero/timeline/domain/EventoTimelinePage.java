package bo.com.ganadero.timeline.domain;

import java.util.List;

public record EventoTimelinePage(List<EventoTimelineAnimal> content, int page, int size,
                                 long totalElements, int totalPages) {
    public static EventoTimelinePage of(List<EventoTimelineAnimal> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new EventoTimelinePage(content, page, size, total, totalPages);
    }
}
