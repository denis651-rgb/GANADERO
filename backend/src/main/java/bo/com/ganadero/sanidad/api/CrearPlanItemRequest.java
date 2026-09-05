package bo.com.ganadero.sanidad.api; import bo.com.ganadero.animales.domain.SexoAnimal; import bo.com.ganadero.sanidad.application.CrearPlanItemCommand; import bo.com.ganadero.sanidad.domain.OrigenRegulatorioActividad; import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.UUID;
public record CrearPlanItemRequest(@NotNull TipoActividadSanitaria tipoActividad,UUID productoId,@Size(max=300) String productoRecomendadoTexto,
 UUID categoriaAnimalId,SexoAnimal sexoAplicable,@PositiveOrZero Integer edadMinDias,@PositiveOrZero Integer edadMaxDias,
 @Positive BigDecimal dosis,@Size(max=30) String unidadDosis,@Positive Integer frecuenciaDias,@PositiveOrZero int diasAlerta,
 @Size(max=60) String viaAdministracion,boolean obligatorio,@NotNull OrigenRegulatorioActividad origenRegulatorio,
 @Size(max=60) String especieAplicable,Boolean permiteEdadDesconocida){CrearPlanItemCommand command(){return new CrearPlanItemCommand(tipoActividad,productoId,
 productoRecomendadoTexto,categoriaAnimalId,sexoAplicable,edadMinDias,edadMaxDias,dosis,unidadDosis,frecuenciaDias,diasAlerta,viaAdministracion,obligatorio,
 origenRegulatorio,especieAplicable==null||especieAplicable.isBlank()?"BOVINO":especieAplicable,Boolean.TRUE.equals(permiteEdadDesconocida));}}
