package bo.com.ganadero.compras.api;

import jakarta.validation.constraints.NotNull;

public record ConfirmarCompraRequest(@NotNull Long version) {
}
