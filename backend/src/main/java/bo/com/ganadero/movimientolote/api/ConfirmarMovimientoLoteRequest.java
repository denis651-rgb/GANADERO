package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.ConfirmarMovimientoLoteCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConfirmarMovimientoLoteRequest(
        @NotNull Long version,
        @NotEmpty List<UUID> animalIds,
        List<@Valid AutorizacionRequest> autorizaciones) {
    public ConfirmarMovimientoLoteCommand command() {
        return new ConfirmarMovimientoLoteCommand(version, animalIds,
                autorizaciones == null ? List.of() : autorizaciones.stream().map(AutorizacionRequest::command).toList());
    }
}
