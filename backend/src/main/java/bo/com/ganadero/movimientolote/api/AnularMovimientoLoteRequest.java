package bo.com.ganadero.movimientolote.api;

import jakarta.validation.constraints.NotBlank;

public record AnularMovimientoLoteRequest(@NotBlank String motivo) {
}
