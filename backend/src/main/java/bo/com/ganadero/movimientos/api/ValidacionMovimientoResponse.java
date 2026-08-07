package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.application.ValidacionMovimiento;

import java.util.List;

public record ValidacionMovimientoResponse(boolean valid, long total, long validos, long invalidos,
                                           List<ValidacionAnimalResponse> resultados) {
    public static ValidacionMovimientoResponse from(ValidacionMovimiento validacion) {
        return new ValidacionMovimientoResponse(validacion.valido(), validacion.total(), validacion.validos(),
                validacion.invalidos(),
                validacion.resultados().stream().map(ValidacionAnimalResponse::from).toList());
    }
}
