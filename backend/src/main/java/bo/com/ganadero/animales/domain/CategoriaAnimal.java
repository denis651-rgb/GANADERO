package bo.com.ganadero.animales.domain; import java.util.UUID;
public record CategoriaAnimal(UUID id,UUID empresaId,String codigo,String nombre,String sexoAplicable,Integer edadMinMeses,Integer edadMaxMeses,String descripcion,boolean activo){public boolean appliesTo(SexoAnimal sexo){return "AMBOS".equals(sexoAplicable)||sexo.name().equals(sexoAplicable);}}
