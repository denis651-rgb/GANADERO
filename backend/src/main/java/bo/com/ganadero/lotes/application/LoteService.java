package bo.com.ganadero.lotes.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class LoteService {
    private final LoteRepository lotes;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ApplicationEventPublisher events;

    public LoteService(LoteRepository lotes, AnimalRepository animales, UserContext context,
                       ApplicationEventPublisher events) {
        this.lotes = lotes;
        this.animales = animales;
        this.context = context;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public LotePage list(EstadoLote estado, String search, int page, int size) {
        CurrentUser user = context.requirePermission("LOTE_VER");
        return lotes.findAll(user.empresaId(), estado, search, page, size);
    }

    @Transactional(readOnly = true)
    public Lote get(UUID id) {
        CurrentUser user = context.requirePermission("LOTE_VER");
        return require(id, user.empresaId());
    }

    @Transactional(readOnly = true)
    public List<MembresiaLote> memberships(UUID id, boolean soloActivas) {
        CurrentUser user = context.requirePermission("LOTE_VER");
        require(id, user.empresaId());
        return lotes.findMemberships(id, user.empresaId(), soloActivas);
    }

    @Transactional
    public Lote create(LoteCommand command) {
        CurrentUser user = context.requirePermission("LOTE_CREAR");
        context.requirePropertyAccess(user, command.propiedadId());
        Lote value = new Lote(UUID.randomUUID(), user.empresaId(), command.propiedadId(), command.codigo(),
                command.nombre(), command.descripcion(), EstadoLote.ABIERTO,
                command.fechaApertura() == null ? LocalDate.now() : command.fechaApertura(), null, 0);
        Lote saved = lotes.create(value, user.userId());
        audit(user, "CREAR", saved.id());
        return saved;
    }

    @Transactional
    public Lote update(UUID id, LoteCommand command) {
        CurrentUser user = context.requirePermission("LOTE_EDITAR");
        Lote old = require(id, user.empresaId());
        if (old.estado() == EstadoLote.CERRADO) throw new BusinessException(ErrorCode.LOT_ALREADY_CLOSED);
        UUID property = command.propiedadId() == null ? old.propiedadId() : command.propiedadId();
        context.requirePropertyAccess(user, property);
        Lote value = new Lote(id, user.empresaId(), property,
                command.codigo() == null ? old.codigo() : command.codigo(),
                command.nombre() == null ? old.nombre() : command.nombre(),
                command.descripcion() == null ? old.descripcion() : command.descripcion(),
                old.estado(), old.fechaApertura(), old.fechaCierre(), old.version());
        Lote saved = lotes.update(value, user.userId());
        audit(user, "ACTUALIZAR", saved.id());
        return saved;
    }

    @Transactional
    public Lote close(UUID id, long version) {
        CurrentUser user = context.requirePermission("LOTE_EDITAR");
        require(id, user.empresaId());
        if (lotes.hasActiveAnimals(id, user.empresaId())) {
            throw new BusinessException(ErrorCode.LOT_HAS_ACTIVE_ANIMALS);
        }
        Lote saved = lotes.close(id, user.empresaId(), version, user.userId());
        audit(user, "CERRAR", saved.id());
        return saved;
    }

    @Transactional
    public List<MembresiaLote> addAnimals(UUID loteId, List<UUID> animalIds) {
        CurrentUser user = context.requirePermission("LOTE_ASIGNAR_ANIMALES");
        Lote lote = require(loteId, user.empresaId());
        if (lote.estado() != EstadoLote.ABIERTO) throw new BusinessException(ErrorCode.LOT_ALREADY_CLOSED);
        context.requirePropertyAccess(user, lote.propiedadId());
        if (animalIds == null || animalIds.isEmpty()) throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        Set<UUID> uniqueIds = new LinkedHashSet<>(animalIds);
        for (UUID animalId : uniqueIds) {
            Animal animal = requireAnimal(user, animalId);
            if (animal.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        }
        List<UUID> alreadyAssigned = lotes.findActiveAnimalsInLote(new ArrayList<>(uniqueIds), user.empresaId());
        if (!alreadyAssigned.isEmpty()) throw new BusinessException(ErrorCode.ANIMAL_ALREADY_IN_ACTIVE_LOT);
        for (UUID animalId : uniqueIds) {
            lotes.openMembership(loteId, animalId, user.empresaId(), user.userId());
            animales.updateLote(animalId, user.empresaId(), loteId, user.userId());
        }
        audit(user, "ASIGNAR_ANIMALES", loteId);
        return lotes.findMemberships(loteId, user.empresaId(), true);
    }

    @Transactional
    public List<MembresiaLote> removeAnimals(UUID loteId, List<UUID> animalIds, String motivo) {
        CurrentUser user = context.requirePermission("LOTE_ASIGNAR_ANIMALES");
        Lote lote = require(loteId, user.empresaId());
        context.requirePropertyAccess(user, lote.propiedadId());
        if (animalIds == null || animalIds.isEmpty()) throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        Set<UUID> uniqueIds = new LinkedHashSet<>(animalIds);
        for (UUID animalId : uniqueIds) {
            List<UUID> active = lotes.findActiveAnimalsInLote(List.of(animalId), user.empresaId());
            if (active.isEmpty()) throw new BusinessException(ErrorCode.ANIMAL_NOT_IN_LOT);
            lotes.closeMembership(loteId, animalId, user.empresaId(), motivo, user.userId());
            animales.updateLote(animalId, user.empresaId(), null, user.userId());
        }
        audit(user, "RETIRAR_ANIMALES", loteId);
        return lotes.findMemberships(loteId, user.empresaId(), true);
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private Lote require(UUID id, UUID empresa) {
        return lotes.findById(id, empresa).orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new LoteAuditEvent(user.empresaId(), user.userId(), accion, "LOTE", id, Instant.now()));
    }
}
