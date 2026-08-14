package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record CeloPage(List<Celo> content, int page, int size, long totalElements, int totalPages) {
    public static CeloPage of(List<Celo> content, int page, int size, long total) {
        return new CeloPage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
