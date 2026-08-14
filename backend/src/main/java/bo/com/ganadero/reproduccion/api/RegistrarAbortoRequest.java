package bo.com.ganadero.reproduccion.api;
import bo.com.ganadero.reproduccion.application.RegistrarAbortoCommand; import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.UUID;
public record RegistrarAbortoRequest(@NotNull UUID animalId,UUID gestacionId,UUID servicioId,@NotNull @PastOrPresent LocalDate fechaEvento,
 @PositiveOrZero Integer edadGestacionalEstimada,@Size(max=300) String causa,@Size(max=1000) String diagnostico,UUID veterinarioId,
 @Size(max=1000) String observaciones){RegistrarAbortoCommand command(){return new RegistrarAbortoCommand(animalId,gestacionId,servicioId,
 fechaEvento,edadGestacionalEstimada,causa,diagnostico,veterinarioId,observaciones);}}
