package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ParentescoService {
    private final ParentescoRepository parentescos;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;

    public ParentescoService(ParentescoRepository parentescos, AnimalRepository animales,
                             UserContext context, ApplicationEventPublisher events,
                             TimelineEventPublisher timeline) {
        this.parentescos = parentescos;
        this.animales = animales;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
    }

    @Transactional(readOnly = true)
    public List<Parentesco> list(UUID animalId) {
        CurrentUser user = context.requirePermission("ANIMAL_VER");
        requireAnimal(user, animalId);
        return parentescos.findByAnimal(animalId, user.empresaId());
    }

    @Transactional
    public Parentesco create(UUID animalId, ParentescoCommand command) {
        CurrentUser user = context.requirePermission("ANIMAL_EDITAR");
        Animal animal = requireAnimal(user, animalId);
        validate(user, animal, command);
        Parentesco value = new Parentesco(
                UUID.randomUUID(), user.empresaId(), animalId, command.tipo(), command.animalPadreId(),
                command.nombreExterno(), command.razaExternaId(), command.registroGenealogico(),
                Instant.now(), user.userId());
        Parentesco saved = parentescos.create(value, user.userId());
        publicar(user, saved, TipoEventoAnimal.GENEALOGIA_REGISTRADA,
                "Se registró el progenitor " + saved.tipo().name().toLowerCase() + ".");
        audit(user, "CREAR", saved.id(), animalId);
        return saved;
    }

    @Transactional
    public Parentesco update(UUID animalId, UUID parentescoId, ParentescoCommand command) {
        CurrentUser user = context.requirePermission("ANIMAL_EDITAR");
        Animal animal = requireAnimal(user, animalId);
        Parentesco current = parentescos.findById(parentescoId, animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARENTESCO_NOT_FOUND));
        ParentescoCommand merged = new ParentescoCommand(
                command.tipo() == null ? current.tipo() : command.tipo(),
                command.animalPadreId() == null ? current.animalPadreId() : command.animalPadreId(),
                command.nombreExterno() == null ? current.nombreExterno() : command.nombreExterno(),
                command.razaExternaId() == null ? current.razaExternaId() : command.razaExternaId(),
                command.registroGenealogico() == null ? current.registroGenealogico() : command.registroGenealogico());
        validate(user, animal, merged);
        Parentesco value = new Parentesco(current.id(), current.empresaId(), animalId, merged.tipo(),
                merged.animalPadreId(), merged.nombreExterno(), merged.razaExternaId(),
                merged.registroGenealogico(), current.fechaRegistro(), user.userId());
        Parentesco saved = parentescos.update(value, user.userId());
        publicar(user, saved, TipoEventoAnimal.GENEALOGIA_ACTUALIZADA,
                "Se actualizó el progenitor " + saved.tipo().name().toLowerCase() + ".");
        audit(user, "ACTUALIZAR", saved.id(), animalId);
        return saved;
    }

    @Transactional
    public void delete(UUID animalId, UUID parentescoId) {
        CurrentUser user = context.requirePermission("ANIMAL_EDITAR");
        requireAnimal(user, animalId);
        parentescos.findById(parentescoId, animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARENTESCO_NOT_FOUND));
        parentescos.delete(parentescoId, animalId, user.empresaId());
        audit(user, "ELIMINAR", parentescoId, animalId);
    }

    private void validate(CurrentUser user, Animal animal, ParentescoCommand command) {
        if (command.animalPadreId() != null) {
            if (command.animalPadreId().equals(animal.id())) throw new BusinessException(ErrorCode.PARENTESCO_CYCLE);
            Animal padre = animales.findById(command.animalPadreId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
            if (command.tipo() == TipoParentesco.MADRE && padre.sexo() != SexoAnimal.HEMBRA) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "La madre registrada debe ser hembra.");
            }
            if (command.tipo() == TipoParentesco.PADRE && padre.sexo() != SexoAnimal.MACHO) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "El padre registrado debe ser macho.");
            }
            assertNoCycle(animal.id(), command.animalPadreId(), user.empresaId());
        } else if (command.nombreExterno() == null && command.razaExternaId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Debe indicar un progenitor registrado o un progenitor externo.");
        }
        if (command.tipo() == null) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    private void assertNoCycle(UUID animalId, UUID padreId, UUID empresa) {
        Set<UUID> visited = new HashSet<>();
        UUID current = padreId;
        int depth = 0;
        while (current != null) {
            if (current.equals(animalId)) throw new BusinessException(ErrorCode.PARENTESCO_CYCLE);
            if (!visited.add(current) || depth++ > 30) return;
            current = parentescos.findRegisteredParentId(current, empresa).orElse(null);
        }
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private void publicar(CurrentUser user, Parentesco parentesco, TipoEventoAnimal tipo, String descripcion) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", parentesco.tipo().name());
        if (parentesco.animalPadreId() != null) {
            metadata.put("animalPadreId", parentesco.animalPadreId().toString());
        }
        if (parentesco.nombreExterno() != null) {
            metadata.put("nombreExterno", parentesco.nombreExterno());
        }
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), parentesco.animalId(), tipo, null,
                descripcion, null, parentesco.id(), metadata, user.userId(), Instant.now(), null));
    }

    private void audit(CurrentUser user, String accion, UUID id, UUID animalId) {
        events.publishEvent(new AnimalAuditEvent(user.empresaId(), user.userId(), accion, "PARENTESCO", id, Instant.now()));
    }
}
