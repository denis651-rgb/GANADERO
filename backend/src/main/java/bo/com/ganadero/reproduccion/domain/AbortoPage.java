package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record AbortoPage(List<Aborto> content, int page, int size, long totalElements, int totalPages) {
    public static AbortoPage of(List<Aborto> content, int page, int size, long total) {
        return new AbortoPage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
