package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.PesajePage;
import java.util.List;

public record PesajePageResponse(List<PesajeResponse> content, int page, int size, long totalElements, int totalPages) {
    public static PesajePageResponse from(PesajePage p) {
        return new PesajePageResponse(p.content().stream().map(PesajeResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
