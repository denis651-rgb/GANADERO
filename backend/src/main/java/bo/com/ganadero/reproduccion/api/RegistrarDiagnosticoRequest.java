package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.application.RegistrarDiagnosticoCommand;
import bo.com.ganadero.reproduccion.domain.MetodoDiagnostico;
import bo.com.ganadero.reproduccion.domain.ResultadoGestacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record RegistrarDiagnosticoRequest(
        UUID id,
        @NotNull UUID animalId,
        UUID servicioId,
        @NotNull Instant fechaDiagnostico,
        @NotNull ResultadoGestacion resultado,
        MetodoDiagnostico metodo,
        Integer diasGestacionEstimados,
        UUID veterinarioId,
        @Size(max = 1000) String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        @Size(max = 200) String idempotencyKey) {

    RegistrarDiagnosticoCommand command() {
        return new RegistrarDiagnosticoCommand(id, animalId, servicioId, fechaDiagnostico, resultado,
                metodo, diasGestacionEstimados, veterinarioId, observaciones, propiedadId, potreroId, loteId,
                clienteUuid, idempotencyKey);
    }
}
