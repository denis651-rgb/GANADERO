package bo.com.ganadero.pesajes.api;

import java.util.List;

public record PesajeSinPesajePageResponse(
        List<PesajeSinPesajeResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static PesajeSinPesajePageResponse of(List<PesajeSinPesajeResponse> content, int page, int size, long total) {
        return new PesajeSinPesajePageResponse(content, page, size, total,
                (int) Math.ceil((double) total / size));
    }
}
