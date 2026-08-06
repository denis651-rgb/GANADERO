package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.application.LoteCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ActualizarLoteRequest(
        UUID propiedadId,
        @Size(max = 60) String codigo,
        @Size(max = 160) String nombre,
        @Size(max = 1000) String descripcion,
        LocalDate fechaApertura,
        @NotNull Long version) {

    public LoteCommand command() {
        return new LoteCommand(propiedadId, codigo, nombre, descripcion, fechaApertura);
    }
}
