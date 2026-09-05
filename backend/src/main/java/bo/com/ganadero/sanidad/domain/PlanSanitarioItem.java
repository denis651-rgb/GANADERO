package bo.com.ganadero.sanidad.domain; import bo.com.ganadero.animales.domain.SexoAnimal; import java.math.BigDecimal; import java.util.UUID;
public record PlanSanitarioItem(UUID id,UUID empresaId,UUID planId,TipoActividadSanitaria tipoActividad,UUID productoId,
 String productoRecomendadoTexto,UUID categoriaAnimalId,SexoAnimal sexoAplicable,Integer edadMinDias,Integer edadMaxDias,
 BigDecimal dosis,String unidadDosis,Integer frecuenciaDias,int diasAlerta,String viaAdministracion,boolean obligatorio,
 boolean activo,long version,OrigenRegulatorioActividad origenRegulatorio,String especieAplicable,
 boolean permiteEdadDesconocida) {
  public PlanSanitarioItem(UUID id,UUID empresaId,UUID planId,TipoActividadSanitaria tipoActividad,UUID productoId,
 String productoRecomendadoTexto,UUID categoriaAnimalId,SexoAnimal sexoAplicable,Integer edadMinDias,Integer edadMaxDias,
 BigDecimal dosis,String unidadDosis,Integer frecuenciaDias,int diasAlerta,String viaAdministracion,boolean obligatorio,
 boolean activo,long version,OrigenRegulatorioActividad origenRegulatorio,String especieAplicable) {
    this(id,empresaId,planId,tipoActividad,productoId,productoRecomendadoTexto,categoriaAnimalId,sexoAplicable,edadMinDias,edadMaxDias,dosis,unidadDosis,frecuenciaDias,diasAlerta,viaAdministracion,obligatorio,activo,version,origenRegulatorio,especieAplicable,false);
  }
}
