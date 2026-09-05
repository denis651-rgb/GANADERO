package bo.com.ganadero.movimientolote.domain;

import java.time.Instant;
import java.util.UUID;

public record PreparacionMovimientoLote(
        UUID id,
        UUID loteOrigenId,
        UUID propiedadOrigenId,
        UUID potreroOrigenId,
        ModalidadMovimientoLote modalidad,
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        AccionLote accionLote,
        UUID loteDestinoId,
        String nuevoLoteNombre,
        String nuevoLoteCodigo,
        String nuevoLoteDescripcion,
        Instant fechaEfectiva,
        String motivo,
        String observaciones,
        EstadoPreparacionLote estado,
        Instant fechaCaptura,
        Instant fechaExpiracion,
        UUID movimientoResultanteId,
        UUID loteResultanteId,
        UUID createdBy,
        long version) {
}
