package bo.com.ganadero.potreros.application; import bo.com.ganadero.potreros.domain.EstadoPotrero; import java.math.BigDecimal; import java.util.UUID;
public record PotreroCommand(UUID propiedadId,UUID sectorId,String codigo,String nombre,BigDecimal superficieHa,
 UUID tipoPastoId,BigDecimal capacidadUa,Boolean tieneAgua,EstadoPotrero estado,String geometriaWkt,Boolean activo,Long version) {}
