package bo.com.ganadero.movimientos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevertirMovimientoRequest(
        @NotBlank(message = "El motivo de la reversión es requerido") String motivo,
        @NotNull @Min(0) Long version) {
}
