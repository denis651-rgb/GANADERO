package bo.com.ganadero.animales.domain; import java.util.*;
public interface AnimalRepository {AnimalPage findAll(UUID empresa,Set<UUID> permitidas,AnimalFilter filter);Optional<Animal> findById(UUID id,UUID empresa);List<AnimalEvent> findEvents(UUID animalId,UUID empresa);
 Animal create(Animal animal,UUID actor);Animal update(Animal animal,UUID actor);Animal changeState(UUID id,UUID empresa,EstadoAnimal from,EstadoAnimal to,String motivo,long version,UUID actor);
 boolean validLocation(UUID empresa,UUID propiedad,UUID potrero);
 void updateLote(UUID animalId,UUID empresa,UUID loteId,UUID actor);
 void move(UUID animalId,UUID empresa,UUID propiedadId,UUID potreroId,UUID loteId,UUID actor);}
