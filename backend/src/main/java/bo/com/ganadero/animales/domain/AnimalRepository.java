package bo.com.ganadero.animales.domain; import java.time.*; import java.util.*;
public interface AnimalRepository {AnimalPage findAll(UUID empresa,Set<UUID> permitidas,AnimalFilter filter);Optional<Animal> findById(UUID id,UUID empresa);Optional<Animal> findByIdAnyCompany(UUID id);Optional<Animal> findByIdForUpdate(UUID id,UUID empresa);List<AnimalEvent> findEvents(UUID animalId,UUID empresa);AnimalEventPage findEventsPage(UUID animalId,UUID empresa,String tipo,Instant desde,Instant hasta,int page,int size);
 Animal create(Animal animal,UUID actor);Animal update(Animal animal,UUID actor);Animal changeState(UUID id,UUID empresa,EstadoAnimal from,EstadoAnimal to,String motivo,long version,UUID actor);
 boolean validLocation(UUID empresa,UUID propiedad,UUID potrero);
 void updateLote(UUID animalId,UUID empresa,UUID loteId,UUID actor);
 void updateFotoPrincipal(UUID animalId,UUID empresa,String path,UUID actor);
 void move(UUID animalId,UUID empresa,UUID propiedadId,UUID potreroId,UUID loteId,UUID actor);
 void restoreLocation(UUID animalId,UUID empresa,UUID propiedadId,UUID potreroId,UUID loteId,UUID actor);
 List<Animal> findEligible(UUID empresa,UUID propiedad,UUID lote,UUID categoria,SexoAnimal sexo);}
