package bo.com.ganadero.potreros.domain; import java.math.BigDecimal; import java.util.UUID;
public record Potrero(UUID id,UUID empresaId,UUID propiedadId,UUID sectorId,String codigo,String nombre,
 BigDecimal superficieHa,UUID tipoPastoId,BigDecimal capacidadUa,boolean tieneAgua,EstadoPotrero estado,
 String geometriaWkt,boolean activo,long version) {}
