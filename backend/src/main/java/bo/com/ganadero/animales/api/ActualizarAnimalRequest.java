package bo.com.ganadero.animales.api; import bo.com.ganadero.animales.application.AnimalCommand; import bo.com.ganadero.animales.domain.*; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record ActualizarAnimalRequest(@Size(max=60) String codigo,@Size(max=160) String nombre,SexoAnimal sexo,
 LocalDate fechaNacimiento,Boolean fechaNacimientoEstimada,UUID razaPrincipalId,UUID categoriaActualId,String color,
 PropositoAnimal proposito,UUID propiedadActualId,UUID potreroActualId,LocalDate fechaIngreso,
 @PositiveOrZero BigDecimal precioAdquisicion,@PositiveOrZero BigDecimal pesoNacimientoKg,
 @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal condicionCorporalActual,String fotoPrincipalPath,String observaciones,
 @NotNull Long version){AnimalCommand command(){return new AnimalCommand(codigo,nombre,sexo,fechaNacimiento,fechaNacimientoEstimada,razaPrincipalId,categoriaActualId,color,proposito,null,propiedadActualId,potreroActualId,null,fechaIngreso,precioAdquisicion,pesoNacimientoKg,condicionCorporalActual,fotoPrincipalPath,observaciones,version);}}
