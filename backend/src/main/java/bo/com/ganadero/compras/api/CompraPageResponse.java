package bo.com.ganadero.compras.api;

import bo.com.ganadero.compras.domain.CompraPage;

import java.util.List;

public record CompraPageResponse(List<CompraResponse> content, int page, int size, long totalElements, int totalPages) {
    public static CompraPageResponse from(CompraPage p) {
        return new CompraPageResponse(p.content().stream().map(CompraResponse::from).toList(), p.page(), p.size(),
                p.totalElements(), p.totalPages());
    }
}
