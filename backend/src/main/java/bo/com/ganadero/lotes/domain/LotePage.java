package bo.com.ganadero.lotes.domain;

import java.util.List;

public record LotePage(List<Lote> content, int page, int size, long totalElements, int totalPages) {
    public static LotePage of(List<Lote> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new LotePage(content, page, size, total, totalPages);
    }
}
