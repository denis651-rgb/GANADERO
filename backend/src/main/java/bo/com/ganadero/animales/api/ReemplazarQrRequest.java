package bo.com.ganadero.animales.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReemplazarQrRequest(
        @NotBlank @Size(max = 1000) String motivo,
        Boolean principal,
        @NotNull Long version) {
}
