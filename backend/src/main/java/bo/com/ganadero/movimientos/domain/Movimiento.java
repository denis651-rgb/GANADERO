package bo.com.ganadero.movimientos.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Movimiento(
        UUID id,
        UUID empresaId,
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
}
