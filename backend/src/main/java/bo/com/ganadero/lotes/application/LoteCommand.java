package bo.com.ganadero.lotes.application;

import java.time.LocalDate;
import java.util.UUID;

public record LoteCommand(
        UUID propiedadId,
        String codigo,
        String nombre,
        String descripcion,
        LocalDate fechaApertura,
        Integer cantidadMaxima,
        Long version) {
    public LoteCommand(UUID propiedadId, String codigo, String nombre, String descripcion, LocalDate fechaApertura) {
        this(propiedadId, codigo, nombre, descripcion, fechaApertura, null, null);
    }
}
