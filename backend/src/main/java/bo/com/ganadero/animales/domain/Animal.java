package bo.com.ganadero.animales.domain; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record Animal(UUID id,UUID empresaId,String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,
 boolean fechaNacimientoEstimada,UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,
 OrigenAnimal origen,UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,EstadoAnimal estado,
 LocalDate fechaIngreso,BigDecimal precioAdquisicion,BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,
 String fotoPrincipalPath,String observaciones,long version, BigDecimal pesoIngresoKg, Boolean pesoIngresoEstimado,
 Integer edadDeclaradaValor, UnidadEdadDeclarada edadDeclaradaUnidad, LocalDate fechaReferenciaEdad,
 FuenteEdadDeclarada fuenteEdadDeclarada, String observacionEstimacion) {
  public Animal(UUID id,UUID empresaId,String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,
 boolean fechaNacimientoEstimada,UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,
 OrigenAnimal origen,UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,EstadoAnimal estado,
 LocalDate fechaIngreso,BigDecimal precioAdquisicion,BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,
 String fotoPrincipalPath,String observaciones,long version) {
    this(id,empresaId,codigo,nombre,sexo,fechaNacimiento,fechaNacimientoEstimada,razaPrincipalId,categoriaActualId,color,proposito,origen,propiedadActualId,potreroActualId,loteActualId,estado,fechaIngreso,precioAdquisicion,pesoNacimientoKg,condicionCorporalActual,fotoPrincipalPath,observaciones,version, null, null, null, null, null, null, null);
  }
  public Animal(UUID id,UUID empresaId,String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,
 boolean fechaNacimientoEstimada,UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,
 OrigenAnimal origen,UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,EstadoAnimal estado,
 LocalDate fechaIngreso,BigDecimal precioAdquisicion,BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,
 String fotoPrincipalPath,String observaciones,long version, BigDecimal pesoIngresoKg, Boolean pesoIngresoEstimado) {
    this(id,empresaId,codigo,nombre,sexo,fechaNacimiento,fechaNacimientoEstimada,razaPrincipalId,categoriaActualId,color,proposito,origen,propiedadActualId,potreroActualId,loteActualId,estado,fechaIngreso,precioAdquisicion,pesoNacimientoKg,condicionCorporalActual,fotoPrincipalPath,observaciones,version, pesoIngresoKg, pesoIngresoEstimado, null, null, null, null, null);
  }
}
