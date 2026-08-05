package bo.com.ganadero.movimientos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnularMovimientoRequest(
        @NotBlank(message = "El motivo de anulación es requerido") String motivo,
        @NotNull @Min(0) Long version) {
}
