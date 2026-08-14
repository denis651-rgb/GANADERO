package bo.com.ganadero.potreros.api; import bo.com.ganadero.potreros.application.PotreroCommand; import bo.com.ganadero.potreros.domain.EstadoPotrero; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.UUID;
public record CrearPotreroRequest(@NotNull UUID propiedadId,UUID sectorId,@Size(max=60) String codigo,
 @NotBlank @Size(max=160) String nombre,@PositiveOrZero BigDecimal superficieHa,UUID tipoPastoId,
 @PositiveOrZero BigDecimal capacidadUa,Boolean tieneAgua,EstadoPotrero estado,String geometriaWkt){PotreroCommand command(){return new PotreroCommand(propiedadId,sectorId,codigo,nombre,superficieHa,tipoPastoId,capacidadUa,tieneAgua,estado,geometriaWkt,true,false,false,false,false,0L);}}
