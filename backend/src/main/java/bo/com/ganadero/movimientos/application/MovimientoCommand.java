package bo.com.ganadero.movimientos.application;

import bo.com.ganadero.movimientos.domain.TipoMovimiento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovimientoCommand(
        UUID id,
        TipoMovimiento tipo,
        LocalDate fechaMovimiento,
        String motivo,
        UUID origenPropiedadId,
        UUID origenPotreroId,
        UUID origenLoteId,
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        UUID destinoLoteId,
        List<UUID> animalIds) {
}
