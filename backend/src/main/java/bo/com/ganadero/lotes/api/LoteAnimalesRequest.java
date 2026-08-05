package bo.com.ganadero.lotes.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record LoteAnimalesRequest(
        @NotEmpty List<UUID> animalIds,
        @Size(max = 1000) String motivo) {
}
