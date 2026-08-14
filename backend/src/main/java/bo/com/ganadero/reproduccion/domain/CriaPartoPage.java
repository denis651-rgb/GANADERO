package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record CriaPartoPage(List<CriaParto> content, int page, int size, long totalElements, int totalPages) {
    public static CriaPartoPage of(List<CriaParto> content, int page, int size, long total) {
        return new CriaPartoPage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
