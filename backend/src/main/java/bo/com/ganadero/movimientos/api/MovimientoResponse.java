package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MovimientoResponse(
        UUID id,
        TipoMovimiento tipo,
        EstadoMovimiento estado,
        LocalDate fechaMovimiento,
        String motivo,
        UUID origenPropiedadId,
        UUID origenPotreroId,
        UUID origenLoteId,
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        UUID destinoLoteId,
        UUID usuarioCrea,
        UUID usuarioConfirma,
        UUID usuarioAnula,
        Instant fechaConfirmacion,
        Instant fechaAnulacion,
        String motivoAnulacion,
        long version) {

    public static MovimientoResponse from(Movimiento m) {
        return new MovimientoResponse(m.id(), m.tipo(), m.estado(), m.fechaMovimiento(), m.motivo(),
                m.origenPropiedadId(), m.origenPotreroId(), m.origenLoteId(), m.destinoPropiedadId(),
                m.destinoPotreroId(), m.destinoLoteId(), m.usuarioCrea(), m.usuarioConfirma(), m.usuarioAnula(),
                m.fechaConfirmacion(), m.fechaAnulacion(), m.motivoAnulacion(), m.version());
    }
}
