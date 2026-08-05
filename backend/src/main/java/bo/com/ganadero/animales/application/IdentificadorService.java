package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IdentificadorService {
    private final IdentificadorRepository identificadores;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ApplicationEventPublisher events;

    public IdentificadorService(IdentificadorRepository identificadores, AnimalRepository animales,
                                UserContext context, ApplicationEventPublisher events) {
        this.identificadores = identificadores;
        this.animales = animales;
        this.context = context;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<IdentificadorAnimal> list(UUID animalId) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_VER");
        requireAnimal(user, animalId);
        return identificadores.findByAnimal(animalId, user.empresaId());
    }

    @Transactional
    public IdentificadorAnimal assign(UUID animalId, IdentificadorCommand command) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_ASIGNAR");
        Animal animal = requireAnimal(user, animalId);
        if (animal.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        boolean principal = Boolean.TRUE.equals(command.principal());
        if (principal) identificadores.clearPrincipal(animalId, user.empresaId(), null);
        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        IdentificadorAnimal value = new IdentificadorAnimal(
                id, user.empresaId(), animalId, command.tipo(), command.valor(),
                principal, EstadoIdentificador.ACTIVO, Instant.now(), null, null,
                user.userId(), null, command.observaciones(), 0);
        IdentificadorAnimal saved = identificadores.create(value, user.userId());
        audit(user, "ASIGNAR", saved.id());
        return saved;
    }

    @Transactional
    public IdentificadorAnimal update(UUID animalId, UUID identificadorId, IdentificadorCommand command) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_ASIGNAR");
        requireAnimal(user, animalId);
        IdentificadorAnimal current = require(identificadorId, animalId, user.empresaId());
        if (current.estado() == EstadoIdentificador.RETIRADO) throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_RETIRED);
        if (Boolean.TRUE.equals(command.principal())) {
            identificadores.clearPrincipal(animalId, user.empresaId(), identificadorId);
        }
        boolean principal = command.principal() == null ? current.principal() : Boolean.TRUE.equals(command.principal());
        IdentificadorAnimal value = new IdentificadorAnimal(
                current.id(), current.empresaId(), current.animalId(),
                command.tipo() == null ? current.tipo() : command.tipo(),
                command.valor() == null ? current.valor() : command.valor(),
                principal, current.estado(), current.fechaAsignacion(), current.fechaRetiro(),
                current.motivoRetiro(), current.asignadoPor(), current.retiradoPor(),
                command.observaciones() == null ? current.observaciones() : command.observaciones(),
                current.version());
        IdentificadorAnimal saved = identificadores.update(value, user.userId());
        audit(user, "ACTUALIZAR", saved.id());
        return saved;
    }

    @Transactional
    public IdentificadorAnimal retire(UUID animalId, UUID identificadorId, String motivo, long version) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_RETIRAR");
        requireAnimal(user, animalId);
        require(identificadorId, animalId, user.empresaId());
        IdentificadorAnimal saved = identificadores.retire(identificadorId, animalId, user.empresaId(), motivo, version, user.userId());
        audit(user, "RETIRAR", saved.id());
        return saved;
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private IdentificadorAnimal require(UUID id, UUID animalId, UUID empresa) {
        return identificadores.findById(id, animalId, empresa)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new AnimalAuditEvent(user.empresaId(), user.userId(), accion, "IDENTIFICADOR", id, Instant.now()));
    }
}
