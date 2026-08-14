package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.ServicioPage;

import java.util.List;

public record ServicioPageResponse(List<ServicioResponse> content, int page, int size, long totalElements, int totalPages) {
    public static ServicioPageResponse from(ServicioPage p) {
        return new ServicioPageResponse(p.content().stream().map(ServicioResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
