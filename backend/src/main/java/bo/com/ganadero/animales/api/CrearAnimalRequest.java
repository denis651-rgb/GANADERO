package bo.com.ganadero.animales.api; import bo.com.ganadero.animales.application.AnimalCommand; import bo.com.ganadero.animales.domain.*; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record CrearAnimalRequest(@NotBlank @Size(max=60) String codigo,@Size(max=160) String nombre,@NotNull SexoAnimal sexo,
 LocalDate fechaNacimiento,Boolean fechaNacimientoEstimada,@NotNull UUID razaPrincipalId,@NotNull UUID categoriaActualId,
 String color,@NotNull PropositoAnimal proposito,@NotNull OrigenAnimal origen,@NotNull UUID propiedadActualId,
 @NotNull UUID potreroActualId,LocalDate fechaIngreso,@PositiveOrZero BigDecimal precioAdquisicion,
 @PositiveOrZero BigDecimal pesoNacimientoKg,@DecimalMin("1.0") @DecimalMax("5.0") BigDecimal condicionCorporalActual,
  String fotoPrincipalPath,String observaciones){AnimalCommand command(){return new AnimalCommand(null,codigo,nombre,sexo,fechaNacimiento,fechaNacimientoEstimada,razaPrincipalId,categoriaActualId,color,proposito,origen,propiedadActualId,potreroActualId,null,fechaIngreso,precioAdquisicion,pesoNacimientoKg,condicionCorporalActual,fotoPrincipalPath,observaciones,0L);}}
