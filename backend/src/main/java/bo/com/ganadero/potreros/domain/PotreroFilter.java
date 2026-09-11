package bo.com.ganadero.potreros.domain; import java.util.UUID;
public record PotreroFilter(UUID propiedadId,EstadoPotrero estado,UUID sectorId,int page,int size) {}
