package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.application.LoteCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CrearLoteRequest(
        @NotNull UUID propiedadId,
        @NotBlank @Size(max = 60) String codigo,
        @NotBlank @Size(max = 160) String nombre,
        @Size(max = 1000) String descripcion,
        LocalDate fechaApertura) {

    public LoteCommand command() {
        return new LoteCommand(propiedadId, codigo, nombre, descripcion, fechaApertura);
    }
}
