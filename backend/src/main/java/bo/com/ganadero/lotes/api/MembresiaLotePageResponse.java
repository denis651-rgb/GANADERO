package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.domain.MembresiaLotePage;

import java.util.List;

public record MembresiaLotePageResponse(List<MembresiaResponse> content, int page, int size,
                                        long totalElements, int totalPages) {
    public static MembresiaLotePageResponse from(MembresiaLotePage page) {
        return new MembresiaLotePageResponse(page.content().stream().map(MembresiaResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
