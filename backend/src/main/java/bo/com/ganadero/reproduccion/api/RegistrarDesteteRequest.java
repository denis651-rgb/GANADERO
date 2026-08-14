package bo.com.ganadero.reproduccion.api;
import bo.com.ganadero.reproduccion.application.RegistrarDesteteCommand; import bo.com.ganadero.reproduccion.domain.TipoDestete;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record RegistrarDesteteRequest(@NotNull UUID animalCriaId,@NotNull UUID madreId,@NotNull @PastOrPresent LocalDate fechaDestete,
 @Positive BigDecimal pesoDesteteKg,@NotNull TipoDestete tipoDestete,@Size(max=500) String motivo,UUID responsableId,
 @Size(max=1000) String observaciones){RegistrarDesteteCommand command(){return new RegistrarDesteteCommand(animalCriaId,madreId,fechaDestete,
 pesoDesteteKg,tipoDestete,motivo,responsableId,observaciones);}}
