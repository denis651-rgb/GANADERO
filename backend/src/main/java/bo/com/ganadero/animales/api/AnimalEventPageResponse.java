package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.AnimalEventPage;

import java.util.List;

public record AnimalEventPageResponse(List<AnimalEventResponse> content, int page, int size,
                                      long totalElements, int totalPages) {
    public static AnimalEventPageResponse from(AnimalEventPage p) {
        return new AnimalEventPageResponse(p.content().stream().map(AnimalEventResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
