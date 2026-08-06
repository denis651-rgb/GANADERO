package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.MovimientoPage;

import java.util.List;

public record MovimientoPageResponse(List<MovimientoResponse> content, int page, int size, long totalElements, int totalPages) {
    public static MovimientoPageResponse from(MovimientoPage page) {
        return new MovimientoPageResponse(
                page.content().stream().map(MovimientoResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
