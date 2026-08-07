package bo.com.ganadero.animales.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverQrRequest(
        @NotBlank @Size(max = 2048) String payload) {
}
