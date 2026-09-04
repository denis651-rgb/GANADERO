package bo.com.ganadero.propiedades.domain;
import java.util.*;

public interface PropiedadRepository {
 List<Propiedad> findAll(); Optional<Propiedad> findById(UUID id);
 Propiedad create(Propiedad value, UUID actor); Propiedad update(Propiedad value, UUID actor);
 boolean hasActiveAnimals(UUID id);
}
