package bo.com.ganadero.compras.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnularCompraRequest(@NotBlank String motivo, @NotNull Long version) {
}
