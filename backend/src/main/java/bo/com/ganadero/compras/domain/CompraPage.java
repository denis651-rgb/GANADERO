package bo.com.ganadero.compras.domain;

import java.util.List;

public record CompraPage(List<Compra> content, int page, int size, long totalElements, int totalPages) {
    public static CompraPage of(List<Compra> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new CompraPage(content, page, size, totalElements, totalPages);
    }
}
