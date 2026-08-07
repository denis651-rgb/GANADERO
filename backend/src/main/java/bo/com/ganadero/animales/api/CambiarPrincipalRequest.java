package bo.com.ganadero.animales.api;

import jakarta.validation.constraints.NotNull;

public record CambiarPrincipalRequest(
        @NotNull Long version) {
}
