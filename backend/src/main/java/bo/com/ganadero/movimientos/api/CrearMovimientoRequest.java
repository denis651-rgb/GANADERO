package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.application.MovimientoCommand;
import bo.com.ganadero.movimientos.domain.MovimientoAnimal;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CrearMovimientoRequest(
        @NotNull(message = "El tipo de movimiento es requerido") TipoMovimiento tipo,
        LocalDate fechaMovimiento,
        String motivo,
        String observacion,
        UUID origenPropiedadId,
        UUID origenPotreroId,
        UUID origenLoteId,
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        UUID destinoLoteId,
        @NotEmpty(message = "Debe incluir al menos un animal")
        @Valid List<MovimientoAnimalRequest> animales) {

    public record MovimientoAnimalRequest(@NotNull(message = "El animal es requerido") UUID animalId,
                                          Long version) {
        MovimientoAnimal toDomain() {
            return new MovimientoAnimal(animalId, version == null ? 0 : version);
        }
    }

    public MovimientoCommand command() {
        return new MovimientoCommand(null, tipo, fechaMovimiento, motivo, observacion, origenPropiedadId,
                origenPotreroId, origenLoteId, destinoPropiedadId, destinoPotreroId, destinoLoteId,
                animales.stream().map(MovimientoAnimalRequest::toDomain).toList());
    }
}
