package bo.com.ganadero.reproduccion.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AnularReproduccionRequest(
        @NotBlank @Size(max = 1000) String motivo,
        @PositiveOrZero long version) {
}
