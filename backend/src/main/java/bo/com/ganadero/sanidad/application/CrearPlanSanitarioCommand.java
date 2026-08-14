package bo.com.ganadero.sanidad.application; import java.time.LocalDate;
public record CrearPlanSanitarioCommand(String nombre,String descripcion,LocalDate fechaInicio,LocalDate fechaFin) {}
