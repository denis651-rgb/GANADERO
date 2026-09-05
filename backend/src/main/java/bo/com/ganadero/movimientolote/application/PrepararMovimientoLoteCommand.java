package bo.com.ganadero.movimientolote.application;

import bo.com.ganadero.movimientolote.domain.AccionLote;
import bo.com.ganadero.movimientolote.domain.ModalidadMovimientoLote;

import java.time.Instant;
import java.util.UUID;

public record PrepararMovimientoLoteCommand(
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        AccionLote accionLote,
        UUID loteDestinoId,
        NuevoLoteCommand nuevoLote,
        Instant fechaEfectiva,
        String motivo,
        String observaciones,
        ModalidadMovimientoLote modalidadDeclarada) {
}
