package bo.com.ganadero.pesajes.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PesajeRepository {
    PesajePage findAll(UUID empresa, UUID animalId, UUID propiedadId, int page, int size);
    Optional<Pesaje> findById(UUID id, UUID empresa);
    Optional<Pesaje> findByClienteUuid(UUID clienteUuid, UUID empresa);
    List<Pesaje> findByAnimal(UUID animalId, UUID empresa);
    List<UUID> listActiveAnimalsOfLote(UUID loteId, UUID empresa);
    Pesaje create(Pesaje pesaje, UUID actor);
    Pesaje annul(UUID id, UUID empresa, String motivo, long version, UUID actor);
}
