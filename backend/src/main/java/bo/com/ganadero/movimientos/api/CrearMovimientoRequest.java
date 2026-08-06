package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.application.MovimientoCommand;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CrearMovimientoRequest(
        @NotEmpty(message = "El tipo de movimiento es requerido") TipoMovimiento tipo,
        LocalDate fechaMovimiento,
        String motivo,
        UUID origenPropiedadId,
        UUID origenPotreroId,
        UUID origenLoteId,
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        UUID destinoLoteId,
        @NotEmpty(message = "Debe incluir al menos un animal") List<UUID> animalIds) {

    public MovimientoCommand command() {
        return new MovimientoCommand(null, tipo, fechaMovimiento, motivo, origenPropiedadId, origenPotreroId,
                origenLoteId, destinoPropiedadId, destinoPotreroId, destinoLoteId, animalIds);
    }
}
