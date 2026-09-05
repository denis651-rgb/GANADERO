package bo.com.ganadero.movimientolote.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PreparacionMovimientoLoteMiembro(
        UUID id,
        UUID preparacionId,
        UUID animalId,
        String animalCodigo,
        String animalNombre,
        String animalEstado,
        UUID propiedadOrigenId,
        UUID potreroOrigenId,
        UUID loteOrigenId,
        long animalVersion,
        boolean elegible,
        String motivoExclusion,
        boolean seleccionado,
        List<RestriccionSanitaria> restricciones,
        Instant fechaCaptura) {
}
