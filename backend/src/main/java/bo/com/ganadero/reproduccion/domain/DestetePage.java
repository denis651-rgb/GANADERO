package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record DestetePage(List<Destete> content, int page, int size, long totalElements, int totalPages) {
    public static DestetePage of(List<Destete> content, int page, int size, long total) {
        return new DestetePage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
