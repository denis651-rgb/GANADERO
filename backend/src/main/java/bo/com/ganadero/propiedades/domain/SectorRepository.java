package bo.com.ganadero.propiedades.domain;
import java.util.*;
public interface SectorRepository {
 List<Sector> findAll(UUID propiedadId); Optional<Sector> findSectorById(UUID id);
 Sector create(Sector value, UUID actor); Sector update(Sector value, UUID actor);
}
