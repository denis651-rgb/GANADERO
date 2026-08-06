package bo.com.ganadero.movimientos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmarMovimientoRequest(@NotNull @Min(0) Long version) {
}
