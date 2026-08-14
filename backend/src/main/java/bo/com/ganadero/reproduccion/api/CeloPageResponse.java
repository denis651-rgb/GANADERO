package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.CeloPage;

import java.util.List;

public record CeloPageResponse(List<CeloResponse> content, int page, int size, long totalElements, int totalPages) {
    public static CeloPageResponse from(CeloPage p) {
        return new CeloPageResponse(p.content().stream().map(CeloResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
