package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.AutorizacionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AutorizacionRequest(@NotNull UUID animalId, @NotBlank String tipoRestriccion, @NotBlank String motivo) {
    public AutorizacionCommand command() {
        return new AutorizacionCommand(animalId, tipoRestriccion, motivo);
    }
}
