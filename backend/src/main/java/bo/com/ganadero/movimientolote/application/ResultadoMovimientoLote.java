package bo.com.ganadero.movimientolote.application;

import bo.com.ganadero.movimientos.domain.TipoMovimiento;

import java.util.UUID;

public record ResultadoMovimientoLote(
        UUID movimientoId,
        UUID loteOrigenId,
        UUID loteDestinoId,
        int animalesMovidos,
        int animalesPermanecenEnOrigen,
        boolean loteOrigenVacio,
        boolean identidadTransferida,
        TipoMovimiento tipoMovimiento) {
}
