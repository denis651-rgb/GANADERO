package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientos.domain.Movimiento;

import java.util.UUID;

public record AnulacionMovimientoLoteResponse(UUID movimientoId, String estado, UUID movimientoReversionId) {
    public static AnulacionMovimientoLoteResponse from(Movimiento revertido) {
        return new AnulacionMovimientoLoteResponse(revertido.id(), revertido.estado().name(),
                revertido.movimientoReversionId());
    }
}
