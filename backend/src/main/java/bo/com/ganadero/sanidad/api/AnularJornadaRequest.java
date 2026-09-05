package bo.com.ganadero.sanidad.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AnularJornadaRequest(
        @NotBlank @Size(max = 500) String motivo,
        @NotNull @PositiveOrZero Long version
) {
}
