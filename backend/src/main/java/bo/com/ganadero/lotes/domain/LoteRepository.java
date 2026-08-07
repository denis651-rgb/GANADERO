package bo.com.ganadero.lotes.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LoteRepository {
    LotePage findAll(UUID empresa, Set<UUID> propiedades, boolean todas, EstadoLote estado,
                     String search, int page, int size);
    Optional<Lote> findById(UUID id, UUID empresa);
    Lote create(Lote lote, UUID actor);
    Lote update(Lote lote, UUID actor);
    Lote close(UUID id, UUID empresa, long version, LocalDate fechaCierre, String motivoCierre, UUID actor);
    List<MembresiaLote> findMemberships(UUID loteId, UUID empresa, boolean soloActivas);
    Optional<Lote> findActiveLotOfAnimal(UUID animalId, UUID empresa);
    boolean hasActiveAnimals(UUID loteId, UUID empresa);
    Optional<MembresiaLote> findActiveMembership(UUID animalId, UUID empresa);
    MembresiaLote openMembership(UUID loteId, String loteCodigo, UUID animalId, UUID empresa,
                                 String motivoIngreso, String observacion, String modo,
                                 Instant fechaIngreso, UUID actor);
    void closeMembership(UUID loteId, String loteCodigo, UUID animalId, UUID empresa,
                         String motivo, Instant fechaSalida, UUID actor);
    default void closeMembership(UUID loteId, UUID animalId, UUID empresa, String motivo, UUID actor) {
        closeMembership(loteId, null, animalId, empresa, motivo, Instant.now(), actor);
    }
    default MembresiaLote openMembership(UUID loteId, UUID animalId, UUID empresa, UUID actor) {
        return openMembership(loteId, null, animalId, empresa, null, null, "PARCIAL", Instant.now(), actor);
    }
    MembresiaLotePage findHistory(UUID loteId, UUID empresa, UUID animalId, Instant desde, Instant hasta,
                                  String motivoIngreso, String motivoSalida, int page, int size);
}
