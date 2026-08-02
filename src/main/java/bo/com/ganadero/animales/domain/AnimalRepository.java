package bo.com.ganadero.animales.domain; import java.util.*;
public interface AnimalRepository {AnimalPage findAll(UUID empresa,Set<UUID> permitidas,AnimalFilter filter);Optional<Animal> findById(UUID id,UUID empresa);
 Animal create(Animal animal,UUID actor);Animal update(Animal animal,UUID actor);Animal changeState(UUID id,UUID empresa,EstadoAnimal from,EstadoAnimal to,String motivo,long version,UUID actor);
 boolean validLocation(UUID empresa,UUID propiedad,UUID potrero);}
