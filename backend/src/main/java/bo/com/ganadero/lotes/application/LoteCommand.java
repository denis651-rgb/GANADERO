package bo.com.ganadero.lotes.application;

import java.time.LocalDate;
import java.util.UUID;

public record LoteCommand(
        UUID propiedadId,
        String codigo,
        String nombre,
        String descripcion,
        LocalDate fechaApertura) {
}
