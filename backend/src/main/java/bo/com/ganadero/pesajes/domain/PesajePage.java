package bo.com.ganadero.pesajes.domain;

import java.util.List;

public record PesajePage(List<Pesaje> content, int page, int size, long totalElements, int totalPages) {
    public static PesajePage of(List<Pesaje> content, int page, int size, long total) {
        return new PesajePage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
