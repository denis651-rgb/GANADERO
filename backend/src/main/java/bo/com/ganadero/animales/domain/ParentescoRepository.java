package bo.com.ganadero.animales.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentescoRepository {
    List<Parentesco> findByAnimal(UUID animalId, UUID empresa);
    Optional<Parentesco> findById(UUID id, UUID animalId, UUID empresa);
    Optional<UUID> findRegisteredParentId(UUID animalId, UUID empresa);
    Parentesco create(Parentesco parentesco, UUID actor);
    Parentesco update(Parentesco parentesco, UUID actor);
    void delete(UUID id, UUID animalId, UUID empresa);
}
