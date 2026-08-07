package bo.com.ganadero.animales.domain;

import java.util.List;

public record AnimalEventPage(List<AnimalEvent> content, int page, int size, long totalElements, int totalPages) {
    public static AnimalEventPage of(List<AnimalEvent> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new AnimalEventPage(content, page, size, total, totalPages);
    }
}
