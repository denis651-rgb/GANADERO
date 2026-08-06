package bo.com.ganadero.lotes.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoteRepository {
    LotePage findAll(UUID empresa, EstadoLote estado, String search, int page, int size);
    Optional<Lote> findById(UUID id, UUID empresa);
    Lote create(Lote lote, UUID actor);
    Lote update(Lote lote, UUID actor);
    Lote close(UUID id, UUID empresa, long version, UUID actor);
    List<MembresiaLote> findMemberships(UUID loteId, UUID empresa, boolean soloActivas);
    List<UUID> findActiveAnimalsInLote(List<UUID> animalIds, UUID empresa);
    Optional<Lote> findActiveLotOfAnimal(UUID animalId, UUID empresa);
    boolean hasActiveAnimals(UUID loteId, UUID empresa);
    void openMembership(UUID loteId, UUID animalId, UUID empresa, UUID actor);
    void closeMembership(UUID loteId, UUID animalId, UUID empresa, String motivo, UUID actor);
}
