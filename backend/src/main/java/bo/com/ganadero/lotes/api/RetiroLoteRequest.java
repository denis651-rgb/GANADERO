package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.application.RetiroLoteCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RetiroLoteRequest(
        @NotEmpty List<UUID> animalIds,
        Instant fechaSalida,
        @Size(max = 1000) String motivo) {

    public RetiroLoteCommand command() {
        return new RetiroLoteCommand(animalIds, fechaSalida, motivo);
    }
}
