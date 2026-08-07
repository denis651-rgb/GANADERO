package bo.com.ganadero.animales.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RetirarIdentificadorRequest(
        @NotBlank @Size(min = 5, max = 1000) String motivo,
        @NotNull Long version) {
}
