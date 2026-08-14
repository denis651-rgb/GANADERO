package bo.com.ganadero.reproduccion.application;
import java.time.LocalDate; import java.util.UUID;
public record RegistrarAbortoCommand(UUID animalId,UUID gestacionId,UUID servicioId,LocalDate fechaEvento,
 Integer edadGestacionalEstimada,String causa,String diagnostico,UUID veterinarioId,String observaciones) {}
