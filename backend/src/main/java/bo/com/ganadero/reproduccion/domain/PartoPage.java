package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record PartoPage(List<Parto> content, int page, int size, long totalElements, int totalPages) {
    public static PartoPage of(List<Parto> content, int page, int size, long total) {
        return new PartoPage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
