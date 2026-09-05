package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.ResultadoMovimientoLote;

import java.util.UUID;

public record ResultadoMovimientoLoteResponse(
        UUID movimientoId,
        UUID loteOrigenId,
        UUID loteDestinoId,
        int animalesMovidos,
        int animalesPermanecenEnOrigen,
        boolean loteOrigenVacio,
        boolean identidadTransferida,
        String tipoMovimiento) {
    public static ResultadoMovimientoLoteResponse from(ResultadoMovimientoLote r) {
        return new ResultadoMovimientoLoteResponse(r.movimientoId(), r.loteOrigenId(), r.loteDestinoId(),
                r.animalesMovidos(), r.animalesPermanecenEnOrigen(), r.loteOrigenVacio(), r.identidadTransferida(),
                r.tipoMovimiento().name());
    }
}
