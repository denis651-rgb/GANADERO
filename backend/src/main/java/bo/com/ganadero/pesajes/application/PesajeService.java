package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.pesajes.domain.*;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PesajeService {
    private final PesajeRepository pesajes;
    private final AnimalRepository animales;
    private final LoteRepository lotes;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;

    public PesajeService(PesajeRepository pesajes, AnimalRepository animales, LoteRepository lotes,
                         UserContext context, ApplicationEventPublisher events, TimelineEventPublisher timeline) {
        this.pesajes = pesajes;
        this.animales = animales;
        this.lotes = lotes;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
    }

    @Transactional(readOnly = true)
    public PesajePage list(UUID animalId, UUID propiedadId, int page, int size) {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        if (animalId != null) requireAnimal(user, animalId);
        if (propiedadId != null) context.requirePropertyAccess(user, propiedadId);
        return pesajes.findAll(user.empresaId(), animalId, propiedadId, page, size);
    }

    @Transactional(readOnly = true)
    public List<Pesaje> history(UUID animalId) {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        requireAnimal(user, animalId);
        return pesajes.findByAnimal(animalId, user.empresaId());
    }

    @Transactional(readOnly = true)
    public Pesaje get(UUID id) {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        Pesaje pesaje = require(id, user.empresaId());
        Animal animal = requireAnimal(user, pesaje.animalId());
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return pesaje;
    }

    @Transactional
    public Pesaje registrar(PesajeCommand command) {
        CurrentUser user = context.requirePermission("PESAJE_REGISTRAR");
        Animal animal = requireAnimal(user, command.animalId());
        UUID property = command.propiedadId() != null ? command.propiedadId() : animal.propiedadActualId();
        context.requirePropertyAccess(user, property);
        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        UUID clienteUuid = command.clienteUuid() != null ? command.clienteUuid() : id;
        Pesaje value = new Pesaje(id, user.empresaId(), command.animalId(),
                command.fecha() == null ? LocalDate.now() : command.fecha(),
                command.pesoKg(), command.tipo() == null ? TipoPesaje.RUTINA : command.tipo(),
                command.condicionCorporal(), command.bascula(), command.responsableId() != null ? command.responsableId() : user.userId(),
                property, command.potreroId() != null ? command.potreroId() : animal.potreroActualId(),
                command.loteId() != null ? command.loteId() : animal.loteActualId(),
                command.dispositivo(), clienteUuid, command.idempotencyKey(), EstadoPesaje.ACTIVO,
                null, null, null, command.observaciones(), null, null, 0);
        Pesaje saved = pesajes.create(value, user.userId());
        publicar(user, saved, TipoEventoAnimal.PESAJE_REGISTRADO, null, null);
        audit(user, "REGISTRAR", saved.id());
        return saved;
    }

    @Transactional
    public List<Pesaje> registrarLote(PesajeLoteCommand command) {
        CurrentUser user = context.requirePermission("PESAJE_REGISTRAR");
        Lote lote = lotes.findById(command.loteId(), user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
        context.requirePropertyAccess(user, lote.propiedadId());
        List<UUID> animals = pesajes.listActiveAnimalsOfLote(command.loteId(), user.empresaId());
        if (animals.isEmpty()) throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        LocalDate fecha = command.fecha() == null ? LocalDate.now() : command.fecha();
        return animals.stream().map(animalId -> {
            UUID id = UUID.randomUUID();
            Pesaje value = new Pesaje(id, user.empresaId(), animalId, fecha, command.pesoKg(),
                    TipoPesaje.RUTINA, null, null, user.userId(),
                    lote.propiedadId(), null, lote.id(), command.dispositivo(), id,
                    command.idempotencyKey(), EstadoPesaje.ACTIVO, null, null, null,
                    command.observaciones(), null, null, 0);
            Pesaje saved = pesajes.create(value, user.userId());
            publicar(user, saved, TipoEventoAnimal.PESAJE_REGISTRADO, null, null);
            audit(user, "REGISTRAR_LOTE", saved.id());
            return saved;
        }).toList();
    }

    @Transactional
    public Pesaje anular(UUID id, String motivo, long version) {
        CurrentUser user = context.requirePermission("PESAJE_ANULAR");
        Pesaje pesaje = require(id, user.empresaId());
        Animal animal = requireAnimal(user, pesaje.animalId());
        context.requirePropertyAccess(user, animal.propiedadActualId());
        if (pesaje.estado() == EstadoPesaje.ANULADO) throw new BusinessException(ErrorCode.PESAJE_ALREADY_ANNULLED);
        Pesaje saved = pesajes.annul(id, user.empresaId(), motivo, version, user.userId());
        publicar(user, saved, TipoEventoAnimal.PESAJE_ANULADO, motivo,
                "PESAJE_ANULADO|" + saved.id() + "|" + saved.version());
        audit(user, "ANULAR", saved.id());
        return saved;
    }

    private void publicar(CurrentUser user, Pesaje pesaje, TipoEventoAnimal tipo, String motivo,
                          String idempotencyKey) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pesoKg", pesaje.pesoKg());
        metadata.put("fecha", pesaje.fecha().toString());
        metadata.put("tipo", pesaje.tipo().name());
        if (pesaje.condicionCorporal() != null) metadata.put("condicionCorporal", pesaje.condicionCorporal());
        if (pesaje.dispositivo() != null) metadata.put("dispositivo", pesaje.dispositivo());
        if (motivo != null) metadata.put("motivo", motivo);
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), pesaje.animalId(), tipo, null,
                pesaje.pesoKg() + " kg" + (motivo == null ? "" : " · " + motivo), null, pesaje.id(),
                metadata, user.userId(), Instant.now(), idempotencyKey));
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private Pesaje require(UUID id, UUID empresa) {
        return pesajes.findById(id, empresa).orElseThrow(() -> new BusinessException(ErrorCode.PESAJE_NOT_FOUND));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new PesajeAuditEvent(user.empresaId(), user.userId(), accion, "PESAJE", id, Instant.now()));
    }
}
