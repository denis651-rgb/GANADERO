package bo.com.ganadero.propiedades.domain;
import java.util.*;
public interface PropiedadRepository {
 List<Propiedad> findAll(UUID empresaId,Set<UUID> permitidas); Optional<Propiedad> findById(UUID id,UUID empresaId);
 Propiedad create(Propiedad value,UUID actor); Propiedad update(Propiedad value,UUID actor); boolean hasActiveAnimals(UUID id,UUID empresaId);
}
