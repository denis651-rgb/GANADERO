package bo.com.ganadero.animales.application; import bo.com.ganadero.animales.domain.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record AnimalCommand(UUID id, String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,Boolean fechaNacimientoEstimada,
 UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,OrigenAnimal origen,
 UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,LocalDate fechaIngreso,BigDecimal precioAdquisicion,
 BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,String fotoPrincipalPath,String observaciones,Long version, BigDecimal pesoIngresoKg, Boolean pesoIngresoEstimado, Boolean quitarFechaNacimiento, Boolean corregirPesoCompra,
 Integer edadDeclaradaValor, UnidadEdadDeclarada edadDeclaradaUnidad, LocalDate fechaReferenciaEdad, FuenteEdadDeclarada fuenteEdad, String observacionEstimacion, String categoriaManualMotivo, Boolean confirmarFechaNacimiento,
 /** Nombre de una raza nueva a crear (o reutilizar si ya existe) en vez de razaPrincipalId. Ver AnimalService.resolveBreed. */
 String razaNueva) {
  public AnimalCommand(UUID id, String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,Boolean fechaNacimientoEstimada,
 UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,OrigenAnimal origen,
 UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,LocalDate fechaIngreso,BigDecimal precioAdquisicion,
 BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,String fotoPrincipalPath,String observaciones,Long version) {
    this(id,codigo,nombre,sexo,fechaNacimiento,fechaNacimientoEstimada,razaPrincipalId,categoriaActualId,color,proposito,origen,propiedadActualId,potreroActualId,loteActualId,fechaIngreso,precioAdquisicion,pesoNacimientoKg,condicionCorporalActual,fotoPrincipalPath,observaciones,version, null, null, false, false, null, null, null, null, null, null, false, null);
  }
  public AnimalCommand(UUID id, String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,Boolean fechaNacimientoEstimada,
 UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,OrigenAnimal origen,
 UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,LocalDate fechaIngreso,BigDecimal precioAdquisicion,
 BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,String fotoPrincipalPath,String observaciones,Long version, BigDecimal pesoIngresoKg, Boolean pesoIngresoEstimado, Boolean quitarFechaNacimiento, Boolean corregirPesoCompra) {
    this(id,codigo,nombre,sexo,fechaNacimiento,fechaNacimientoEstimada,razaPrincipalId,categoriaActualId,color,proposito,origen,propiedadActualId,potreroActualId,loteActualId,fechaIngreso,precioAdquisicion,pesoNacimientoKg,condicionCorporalActual,fotoPrincipalPath,observaciones,version, pesoIngresoKg, pesoIngresoEstimado, quitarFechaNacimiento, corregirPesoCompra, null, null, null, null, null, null, false, null);
  }
}
