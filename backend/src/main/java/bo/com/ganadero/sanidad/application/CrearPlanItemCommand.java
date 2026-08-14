package bo.com.ganadero.sanidad.application; import bo.com.ganadero.animales.domain.SexoAnimal; import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria; import java.math.BigDecimal; import java.util.UUID;
public record CrearPlanItemCommand(TipoActividadSanitaria tipoActividad,UUID productoId,String productoRecomendadoTexto,
 UUID categoriaAnimalId,SexoAnimal sexoAplicable,Integer edadMinDias,Integer edadMaxDias,BigDecimal dosis,String unidadDosis,
 Integer frecuenciaDias,int diasAlerta,String viaAdministracion,boolean obligatorio) {}
