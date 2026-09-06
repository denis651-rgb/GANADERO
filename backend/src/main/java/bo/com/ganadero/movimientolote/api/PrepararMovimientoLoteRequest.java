package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.PrepararMovimientoLoteCommand;
import bo.com.ganadero.movimientolote.domain.AccionLote;
import bo.com.ganadero.movimientolote.domain.ModalidadMovimientoLote;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record PrepararMovimientoLoteRequest(
        @NotNull UUID destinoPropiedadId,
        @NotNull UUID destinoPotreroId,
        @NotNull AccionLote accionLote,
        UUID loteDestinoId,
        @Valid NuevoLoteRequest nuevoLote,
        Instant fechaEfectiva,
        @Size(max = 1000) String motivo,
        @Size(max = 2000) String observaciones,
        ModalidadMovimientoLote modalidadDeclarada) {
    public PrepararMovimientoLoteCommand command() {
        return new PrepararMovimientoLoteCommand(destinoPropiedadId, destinoPotreroId, accionLote, loteDestinoId,
                nuevoLote == null ? null : nuevoLote.command(), fechaEfectiva, motivo, observaciones, modalidadDeclarada);
    }
}
