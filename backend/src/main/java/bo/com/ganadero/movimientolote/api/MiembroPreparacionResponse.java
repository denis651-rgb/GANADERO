package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.domain.PreparacionMovimientoLoteMiembro;

import java.util.List;
import java.util.UUID;

public record MiembroPreparacionResponse(
        UUID animalId,
        String codigo,
        String nombre,
        String estado,
        UUID propiedadOrigenId,
        UUID potreroOrigenId,
        UUID loteOrigenId,
        long animalVersion,
        boolean elegible,
        String motivoExclusion,
        boolean seleccionadoPorDefecto,
        List<RestriccionSanitariaResponse> restricciones) {
    public static MiembroPreparacionResponse from(PreparacionMovimientoLoteMiembro m) {
        return new MiembroPreparacionResponse(m.animalId(), m.animalCodigo(), m.animalNombre(), m.animalEstado(),
                m.propiedadOrigenId(), m.potreroOrigenId(), m.loteOrigenId(), m.animalVersion(), m.elegible(),
                m.motivoExclusion(), m.seleccionado(), m.restricciones().stream().map(RestriccionSanitariaResponse::from).toList());
    }
}
