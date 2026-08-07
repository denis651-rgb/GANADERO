package bo.com.ganadero.movimientos.domain;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;

public final class MovimientoStatePolicy {
    private MovimientoStatePolicy() {}

    public static boolean can(EstadoMovimiento from, EstadoMovimiento to) {
        return (from == EstadoMovimiento.PENDIENTE && to == EstadoMovimiento.CONFIRMADO)
                || (from == EstadoMovimiento.PENDIENTE && to == EstadoMovimiento.ANULADO)
                || (from == EstadoMovimiento.CONFIRMADO && to == EstadoMovimiento.REVERTIDO);
    }

    public static void require(EstadoMovimiento from, EstadoMovimiento to) {
        if (!can(from, to)) {
            throw new BusinessException(ErrorCode.INVALID_MOVEMENT_STATE_TRANSITION,
                    "No se permite la transición de estado " + from + " a " + to + ".");
        }
    }
}
