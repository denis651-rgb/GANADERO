package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;

import java.time.LocalDate;
import java.util.UUID;

public record LoteResponse(
        UUID id,
        UUID propiedadId,
        String codigo,
        String nombre,
        String descripcion,
        EstadoLote estado,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        long version) {

    public static LoteResponse from(Lote lote) {
        return new LoteResponse(lote.id(), lote.propiedadId(), lote.codigo(), lote.nombre(), lote.descripcion(),
                lote.estado(), lote.fechaApertura(), lote.fechaCierre(), lote.version());
    }
}
