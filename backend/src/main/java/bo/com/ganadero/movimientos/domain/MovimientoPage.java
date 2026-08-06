package bo.com.ganadero.movimientos.domain;

import java.util.List;

public record MovimientoPage(List<Movimiento> content, int page, int size, long totalElements, int totalPages) {
    public static MovimientoPage of(List<Movimiento> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new MovimientoPage(content, page, size, total, totalPages);
    }
}
