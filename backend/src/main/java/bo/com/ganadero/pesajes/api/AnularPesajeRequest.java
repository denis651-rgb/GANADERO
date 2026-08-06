package bo.com.ganadero.pesajes.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnularPesajeRequest(@NotBlank String motivo, @NotNull Long version) {
}
