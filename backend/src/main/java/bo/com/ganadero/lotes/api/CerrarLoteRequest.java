package bo.com.ganadero.lotes.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CerrarLoteRequest(
        @NotNull Long version,
        LocalDate fechaCierre,
        @Size(max = 1000) String motivo) {
}
