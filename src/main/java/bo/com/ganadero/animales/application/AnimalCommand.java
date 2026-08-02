package bo.com.ganadero.animales.application; import bo.com.ganadero.animales.domain.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record AnimalCommand(String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,Boolean fechaNacimientoEstimada,
 UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,OrigenAnimal origen,
 UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,LocalDate fechaIngreso,BigDecimal precioAdquisicion,
 BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,String fotoPrincipalPath,String observaciones,Long version) {}
