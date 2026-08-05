package bo.com.ganadero.lotes.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Lote(
        UUID id,
        UUID empresaId,
        UUID propiedadId,
        String codigo,
        String nombre,
        String descripcion,
        EstadoLote estado,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        long version) {
}
