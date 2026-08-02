package bo.com.ganadero.propiedades.domain;
import java.util.*;
public interface SectorRepository { List<Sector> findAll(UUID propiedadId,UUID empresaId); Optional<Sector> findSectorById(UUID id,UUID empresaId);
 Sector create(Sector value,UUID actor); Sector update(Sector value,UUID actor); }
