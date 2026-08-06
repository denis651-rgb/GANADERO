package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.domain.LotePage;

import java.util.List;

public record LotePageResponse(List<LoteResponse> content, int page, int size,
                               long totalElements, int totalPages) {
    public static LotePageResponse from(LotePage page) {
        return new LotePageResponse(page.content().stream().map(LoteResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
