package bo.com.ganadero.potreros.domain; import java.util.*;
public interface PotreroRepository { List<Potrero> findAll(UUID empresa,Set<UUID> allowed,UUID propiedad,EstadoPotrero estado,UUID sector);
 Optional<Potrero> findById(UUID id,UUID empresa); Potrero create(Potrero p,UUID actor); Potrero update(Potrero p,UUID actor,boolean quitarSector,boolean quitarTipoPasto,boolean quitarSuperficie,boolean quitarCapacidad);
 boolean propertyExists(UUID property,UUID empresa); boolean sectorBelongs(UUID sector,UUID property,UUID empresa);
 boolean grassTypeExists(UUID grass,UUID empresa); boolean hasActiveAnimals(UUID paddock,UUID empresa); }
