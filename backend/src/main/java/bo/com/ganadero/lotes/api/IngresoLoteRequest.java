package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.application.IngresoLoteCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IngresoLoteRequest(
        @NotEmpty List<UUID> animalIds,
        String modo,
        Instant fechaIngreso,
        @Size(max = 1000) String motivo,
        @Size(max = 2000) String observacion) {

    public IngresoLoteCommand command() {
        return new IngresoLoteCommand(animalIds, modo, fechaIngreso, motivo, observacion);
    }
}
