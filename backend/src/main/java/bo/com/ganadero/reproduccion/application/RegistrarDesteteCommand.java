package bo.com.ganadero.reproduccion.application;
import bo.com.ganadero.reproduccion.domain.TipoDestete; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record RegistrarDesteteCommand(UUID animalCriaId,UUID madreId,LocalDate fechaDestete,BigDecimal pesoDesteteKg,
 TipoDestete tipoDestete,String motivo,UUID responsableId,String observaciones) {}
