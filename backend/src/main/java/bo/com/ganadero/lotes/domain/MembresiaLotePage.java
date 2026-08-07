package bo.com.ganadero.lotes.domain;

import java.util.List;

public record MembresiaLotePage(List<MembresiaLote> content, int page, int size, long totalElements, int totalPages) {
    public static MembresiaLotePage of(List<MembresiaLote> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new MembresiaLotePage(content, page, size, total, totalPages);
    }
}
