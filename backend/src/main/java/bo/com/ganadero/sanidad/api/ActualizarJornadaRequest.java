package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.ActualizarJornadaCommand;
import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ActualizarJornadaRequest(
        @NotNull TipoActividadSanitaria tipoJornada,
        @NotNull @PastOrPresent LocalDate fechaInicio,
        @NotNull UUID propiedadId,
        UUID potreroId,
        UUID loteGanaderoId,
        @NotNull UUID responsableId,
        UUID veterinarioId,
        @Size(max = 1000) String observaciones,
        @NotNull @PositiveOrZero Long version
) {
    ActualizarJornadaCommand command() {
        return new ActualizarJornadaCommand(tipoJornada, fechaInicio, propiedadId, potreroId,
                loteGanaderoId, responsableId, veterinarioId, observaciones, version);
    }
}
