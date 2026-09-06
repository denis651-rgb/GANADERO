package bo.com.ganadero.sanidad.api;
import bo.com.ganadero.sanidad.application.CrearPlanSanitarioCommand;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
public record CrearPlanRequest(@NotBlank @Size(max = 160) String nombre, @Size(max = 2000) String descripcion,
                               @NotNull LocalDate fechaInicio, LocalDate fechaFin, UUID propiedadId) {
    CrearPlanSanitarioCommand command() {
        return new CrearPlanSanitarioCommand(nombre, descripcion, fechaInicio, fechaFin, propiedadId);
    }
}
