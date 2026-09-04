package bo.com.ganadero.sanidad.api; import bo.com.ganadero.sanidad.application.RegistrarAplicacionDeclaradaCommand; import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record RegistrarAplicacionDeclaradaRequest(@NotNull UUID animalId,@NotNull TipoActividadSanitaria tipoActividad,
 UUID planItemId,@NotNull @PastOrPresent LocalDate fechaAplicacion,@Positive BigDecimal dosis,@Size(max=30) String unidadDosis,
 @Size(max=300) String productoTexto,@Size(max=1000) String observaciones){
 public RegistrarAplicacionDeclaradaCommand command(){return new RegistrarAplicacionDeclaradaCommand(animalId,tipoActividad,
  planItemId,fechaAplicacion,dosis,unidadDosis,productoTexto,observaciones);}}
