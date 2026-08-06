package bo.com.ganadero.animales.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentificadorRepository {
    List<IdentificadorAnimal> findByAnimal(UUID animalId, UUID empresa);
    Optional<IdentificadorAnimal> findById(UUID id, UUID animalId, UUID empresa);
    IdentificadorAnimal create(IdentificadorAnimal identificador, UUID actor);
    IdentificadorAnimal update(IdentificadorAnimal identificador, UUID actor);
    IdentificadorAnimal retire(UUID id, UUID animalId, UUID empresa, String motivo, long version, UUID actor);
    void clearPrincipal(UUID animalId, UUID empresa, UUID exceptoId);
}
