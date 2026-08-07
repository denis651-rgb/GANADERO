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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IdentificadorService {
    private final IdentificadorRepository identificadores;
    private final AnimalRepository animales;
    private final IdentificadorValueNormalizer normalizer;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;

    public IdentificadorService(IdentificadorRepository identificadores, AnimalRepository animales,
                                IdentificadorValueNormalizer normalizer, UserContext context,
                                ApplicationEventPublisher events, TimelineEventPublisher timeline) {
        this.identificadores = identificadores;
        this.animales = animales;
        this.normalizer = normalizer;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
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
        if (command.tipo() == null) throw new BusinessException(ErrorCode.IDENTIFIER_INVALID_VALUE);
        String valor = normalizer.normalize(command.tipo(), command.valor());
        boolean principal = Boolean.TRUE.equals(command.principal());
        if (principal) {
            identificadores.lockActiveIdentifiers(animalId, user.empresaId());
            identificadores.clearPrincipal(animalId, user.empresaId(), null);
        }
        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        Instant now = Instant.now();
        IdentificadorAnimal value = new IdentificadorAnimal(
                id, user.empresaId(), animalId, command.tipo(), valor,
                principal, EstadoIdentificador.ACTIVO, now, null, null,
                user.userId(), null, command.observaciones(), null, now, now, 0);
        IdentificadorAnimal saved = identificadores.create(value, user.userId());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("estado", saved.estado().name());
        metadata.put("tipo", saved.tipo().name());
        metadata.put("valor", saved.valor());
        metadata.put("principal", saved.principal());
        publicar(user, saved, saved.esQr() ? TipoEventoAnimal.QR_ASIGNADO : TipoEventoAnimal.IDENTIFICADOR_ASIGNADO,
                "Se asignó el identificador " + saved.tipo().name() + " " + saved.valor() + ".", metadata, null);
        audit(user, "ASIGNAR_IDENTIFICADOR", saved.id(), metadata);
        return saved;
    }

    @Transactional
    public IdentificadorAnimal update(UUID animalId, UUID identificadorId, IdentificadorCommand command) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_ASIGNAR");
        requireAnimal(user, animalId);
        IdentificadorAnimal current = require(identificadorId, animalId, user.empresaId());
        if (current.retirado()) throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_RETIRED);
        if (current.esQr()) throw new BusinessException(ErrorCode.IDENTIFIER_QR_IMMUTABLE);
        if (command.tipo() != null && command.tipo() != current.tipo()) {
            throw new BusinessException(ErrorCode.IDENTIFIER_INVALID_VALUE);
        }
        String valor = command.valor() == null ? current.valor() : normalizer.normalize(current.tipo(), command.valor());
        boolean principal = command.principal() == null ? current.principal() : Boolean.TRUE.equals(command.principal());
        if (principal && !current.principal()) {
            identificadores.lockActiveIdentifiers(animalId, user.empresaId());
            identificadores.clearPrincipal(animalId, user.empresaId(), identificadorId);
        }
        long version = command.version() == null ? current.version() : command.version();
        IdentificadorAnimal value = new IdentificadorAnimal(
                current.id(), current.empresaId(), current.animalId(),
                current.tipo(), valor,
                principal, current.estado(), current.fechaAsignacion(), current.fechaRetiro(),
                current.motivoRetiro(), current.asignadoPor(), current.retiradoPor(),
                command.observaciones() == null ? current.observaciones() : command.observaciones(),
                current.payload(), current.createdAt(), current.updatedAt(), version);
        IdentificadorAnimal saved = identificadores.update(value, user.userId());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", saved.tipo().name());
        metadata.put("valorAnterior", current.valor());
        metadata.put("valorNuevo", saved.valor());
        metadata.put("principal", saved.principal());
        publicar(user, saved, TipoEventoAnimal.IDENTIFICADOR_ACTUALIZADO,
                "Se actualizó el identificador " + saved.tipo().name() + " " + saved.valor() + ".", metadata,
                "IDENTIFICADOR_ACTUALIZADO|" + saved.id() + "|" + saved.version());
        audit(user, "ACTUALIZAR_IDENTIFICADOR", saved.id(), metadata);
        return saved;
    }

    @Transactional
    public IdentificadorAnimal makePrincipal(UUID animalId, UUID identificadorId, long version) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_ASIGNAR");
        requireAnimal(user, animalId);
        IdentificadorAnimal current = require(identificadorId, animalId, user.empresaId());
        if (current.retirado()) throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_RETIRED);
        if (current.principal()) return current;
        identificadores.lockActiveIdentifiers(animalId, user.empresaId());
        identificadores.clearPrincipal(animalId, user.empresaId(), identificadorId);
        IdentificadorAnimal saved = identificadores.setPrincipal(identificadorId, animalId, user.empresaId(), version, user.userId());
        publicar(user, saved, TipoEventoAnimal.IDENTIFICADOR_PRINCIPAL,
                "Se marcó el identificador " + saved.tipo().name() + " " + saved.valor() + " como principal.",
                Map.of("tipo", saved.tipo().name(), "valor", saved.valor(), "principal", true),
                "IDENTIFICADOR_PRINCIPAL|" + saved.id() + "|" + saved.version());
        audit(user, "CAMBIAR_PRINCIPAL", saved.id(),
                Map.of("estado", saved.estado().name(), "tipo", saved.tipo().name(), "valor", saved.valor(), "principal", true));
        return saved;
    }

    @Transactional
    public IdentificadorAnimal retire(UUID animalId, UUID identificadorId, String motivo, long version) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_RETIRAR");
        requireAnimal(user, animalId);
        if (motivo == null || motivo.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        IdentificadorAnimal current = require(identificadorId, animalId, user.empresaId());
        if (current.retirado()) throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_RETIRED);
        IdentificadorAnimal saved = identificadores.retire(identificadorId, animalId, user.empresaId(), motivo, version, user.userId());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("estadoAnterior", current.estado().name());
        metadata.put("estadoNuevo", saved.estado().name());
        metadata.put("tipo", saved.tipo().name());
        metadata.put("valor", saved.valor());
        metadata.put("motivo", motivo);
        publicar(user, saved, TipoEventoAnimal.IDENTIFICADOR_RETIRADO,
                "Se retiró el identificador " + saved.tipo().name() + " " + saved.valor() + ".", metadata,
                "IDENTIFICADOR_RETIRADO|" + saved.id() + "|" + saved.version());
        audit(user, "RETIRAR_IDENTIFICADOR", saved.id(), metadata);
        return saved;
    }

    private void publicar(CurrentUser user, IdentificadorAnimal identificador, TipoEventoAnimal tipo,
                          String descripcion, Map<String, Object> metadata, String idempotencyKey) {
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), identificador.animalId(), tipo, null,
                descripcion, null, identificador.id(), metadata, user.userId(), Instant.now(), idempotencyKey));
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

    private void audit(CurrentUser user, String accion, UUID id, Map<String, Object> datos) {
        events.publishEvent(new AnimalAuditEvent(user.empresaId(), user.userId(), accion, "IDENTIFICADOR", id, Instant.now(), datos));
    }
}
