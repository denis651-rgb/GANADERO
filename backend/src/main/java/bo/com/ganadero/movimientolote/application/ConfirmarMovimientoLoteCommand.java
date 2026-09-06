package bo.com.ganadero.movimientolote.application;

import java.util.List;
import java.util.UUID;

public record ConfirmarMovimientoLoteCommand(
        long version,
        List<UUID> animalIds,
        List<AutorizacionCommand> autorizaciones) {
}
