package bo.com.ganadero.animales.domain; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record Animal(UUID id,UUID empresaId,String codigo,String nombre,SexoAnimal sexo,LocalDate fechaNacimiento,
 boolean fechaNacimientoEstimada,UUID razaPrincipalId,UUID categoriaActualId,String color,PropositoAnimal proposito,
 OrigenAnimal origen,UUID propiedadActualId,UUID potreroActualId,UUID loteActualId,EstadoAnimal estado,
 LocalDate fechaIngreso,BigDecimal precioAdquisicion,BigDecimal pesoNacimientoKg,BigDecimal condicionCorporalActual,
 String fotoPrincipalPath,String observaciones,long version) {}
