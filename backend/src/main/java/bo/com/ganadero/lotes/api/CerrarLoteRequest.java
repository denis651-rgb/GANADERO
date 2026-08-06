package bo.com.ganadero.lotes.api;

import jakarta.validation.constraints.NotNull;

public record CerrarLoteRequest(@NotNull Long version) {
}
