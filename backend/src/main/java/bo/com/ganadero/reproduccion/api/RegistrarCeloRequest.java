package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.application.RegistrarCeloCommand;
import bo.com.ganadero.reproduccion.domain.IntensidadCelo;
import bo.com.ganadero.reproduccion.domain.TipoCelo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record RegistrarCeloRequest(
        UUID id,
        @NotNull UUID animalId,
        @NotNull Instant fechaDeteccion,
        @NotNull TipoCelo tipoDeteccion,
        IntensidadCelo intensidad,
        @Size(max = 1000) String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        @Size(max = 200) String idempotencyKey) {

    RegistrarCeloCommand command() {
        return new RegistrarCeloCommand(id, animalId, fechaDeteccion, tipoDeteccion, intensidad, observaciones,
                propiedadId, potreroId, loteId, clienteUuid, idempotencyKey);
    }
}
