package bo.com.ganadero.sanidad.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SeleccionAnimalesRequest(
        @NotNull UUID planItemId,
        @NotNull @PastOrPresent LocalDate fechaAplicacion,
        @NotEmpty List<@NotNull UUID> animalIds
) {
}
