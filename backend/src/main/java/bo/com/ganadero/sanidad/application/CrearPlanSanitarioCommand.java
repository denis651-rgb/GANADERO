package bo.com.ganadero.sanidad.application;

import java.time.LocalDate;
import java.util.UUID;

public record CrearPlanSanitarioCommand(String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin,
                                        UUID propiedadId) {

    /** Constructor legado: alcance global. */
    public CrearPlanSanitarioCommand(String nombre, String descripcion, LocalDate fechaInicio, LocalDate fechaFin) {
        this(nombre, descripcion, fechaInicio, fechaFin, null);
    }
}
