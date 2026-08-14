package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import bo.com.ganadero.timeline.api.TimelinePageResponse;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.application.TimelineService;
import bo.com.ganadero.timeline.domain.EventoTimelineFilter;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class AnimalService {
    private final AnimalRepository animals;
    private final RazaRepository breeds;
    private final CategoriaAnimalRepository categories;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;
    private final TimelineService timelineService;
    private final CodigoService codigos;

    public AnimalService(AnimalRepository a, RazaRepository b, CategoriaAnimalRepository c, UserContext u,
                         ApplicationEventPublisher e, TimelineEventPublisher timeline,
                         TimelineService timelineService, CodigoService codigos) {
        animals = a;
        breeds = b;
        categories = c;
        context = u;
        events = e;
        this.timeline = timeline;
        this.timelineService = timelineService;
        this.codigos = codigos;
    }

    @Transactional(readOnly = true)
    public AnimalPage list(AnimalFilter f) {
        CurrentUser u = context.requirePermission("ANIMAL_VER");
        if (f.propiedadId() != null) context.requirePropertyAccess(u, f.propiedadId());
        if (!u.accesoTodasPropiedades() && u.propiedadesPermitidas().isEmpty()) {
            return AnimalPage.of(List.of(), f.page(), f.size(), 0);
        }
        return animals.findAll(u.empresaId(), u.propiedadesPermitidas(), f);
    }

    @Transactional(readOnly = true)
    public Animal get(UUID id) {
        CurrentUser u = context.requirePermission("ANIMAL_VER");
        Animal a = require(id, u.empresaId());
        context.requirePropertyAccess(u, a.propiedadActualId());
        return a;
    }

    @Transactional(readOnly = true)
    public List<AnimalEvent> history(UUID id) {
        CurrentUser u = context.requirePermission("ANIMAL_VER");
        Animal a = require(id, u.empresaId());
        context.requirePropertyAccess(u, a.propiedadActualId());
        return animals.findEvents(id, u.empresaId());
    }

    @Transactional(readOnly = true)
    public TimelinePageResponse timeline(UUID id, EventoTimelineFilter filtro) {
        CurrentUser u = context.requirePermission("ANIMAL_VER");
        Animal a = require(id, u.empresaId());
        context.requirePropertyAccess(u, a.propiedadActualId());
        return timelineService.timeline(u.empresaId(), a.id(), filtro);
    }

    @Transactional(readOnly = true)
    public List<Raza> breeds() {
        return breeds.findActive(context.requirePermission("ANIMAL_VER").empresaId());
    }

    @Transactional(readOnly = true)
    public List<CategoriaAnimal> categories() {
        return categories.findActive(context.requirePermission("ANIMAL_VER").empresaId());
    }

    @Transactional
    public Animal create(AnimalCommand c) {
        CurrentUser u = context.requirePermission("ANIMAL_CREAR");
        validateReferences(c.razaPrincipalId(), c.categoriaActualId(), c.sexo(), c.propiedadActualId(),
                c.potreroActualId(), u);
        UUID id = c.id() != null ? c.id() : UUID.randomUUID();
        String codigo = codigos.paraCreacion(u, TipoCodigo.ANIMAL, null, null, c.codigo());
        Animal a = new Animal(id, u.empresaId(), codigo, c.nombre(), c.sexo(), c.fechaNacimiento(),
                Boolean.TRUE.equals(c.fechaNacimientoEstimada()), c.razaPrincipalId(), c.categoriaActualId(),
                c.color(), c.proposito(), c.origen(), c.propiedadActualId(), c.potreroActualId(), null,
                EstadoAnimal.ACTIVO, c.fechaIngreso() == null ? LocalDate.now() : c.fechaIngreso(),
                c.precioAdquisicion(), c.pesoNacimientoKg(), c.condicionCorporalActual(), c.fotoPrincipalPath(),
                c.observaciones(), 0);
        Animal saved = animals.create(a, u.userId());
        TipoEventoAnimal tipo = switch (a.origen()) {
            case NACIDO -> TipoEventoAnimal.NACIMIENTO_REGISTRADO;
            case COMPRADO -> TipoEventoAnimal.COMPRA_REGISTRADA;
            case TRANSFERIDO -> TipoEventoAnimal.INGRESO_REGISTRADO;
        };
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codigo", saved.codigo());
        metadata.put("sexo", saved.sexo().name());
        if (saved.propiedadActualId() != null) metadata.put("propiedadId", saved.propiedadActualId().toString());
        timeline.publish(new RegistrarEventoTimeline(u.empresaId(), saved.id(), tipo, null, "Registro inicial",
                null, saved.id(), metadata, u.userId(), Instant.now(), null));
        audit(u, "CREAR", saved.id());
        return saved;
    }

    @Transactional
    public Animal update(UUID id, AnimalCommand c) {
        CurrentUser u = context.requirePermission("ANIMAL_EDITAR");
        Animal old = require(id, u.empresaId());
        context.requirePropertyAccess(u, old.propiedadActualId());
        UUID property = c.propiedadActualId() == null ? old.propiedadActualId() : c.propiedadActualId();
        UUID paddock = c.potreroActualId() == null ? old.potreroActualId() : c.potreroActualId();
        boolean moves = !property.equals(old.propiedadActualId()) || !paddock.equals(old.potreroActualId());
        if (moves && old.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        SexoAnimal sex = c.sexo() == null ? old.sexo() : c.sexo();
        UUID breed = c.razaPrincipalId() == null ? old.razaPrincipalId() : c.razaPrincipalId();
        UUID category = c.categoriaActualId() == null ? old.categoriaActualId() : c.categoriaActualId();
        validateReferences(breed, category, sex, property, paddock, u);
        String codigo = codigos.paraActualizacion(u, TipoCodigo.ANIMAL, null, null, old.codigo(), c.codigo());
        Animal value = new Animal(id, u.empresaId(), codigo, c.nombre(), sex, c.fechaNacimiento(),
                c.fechaNacimientoEstimada() == null ? old.fechaNacimientoEstimada() : c.fechaNacimientoEstimada(),
                breed, category, c.color(), c.proposito() == null ? old.proposito() : c.proposito(), old.origen(),
                property, paddock, old.loteActualId(), old.estado(), c.fechaIngreso(), c.precioAdquisicion(),
                c.pesoNacimientoKg(), c.condicionCorporalActual(), c.fotoPrincipalPath(), c.observaciones(),
                Objects.requireNonNull(c.version()));
        Animal saved = animals.update(value, u.userId());
        timeline.publish(new RegistrarEventoTimeline(u.empresaId(), id, TipoEventoAnimal.ANIMAL_ACTUALIZADO,
                null, "Datos del animal actualizados", null, id,
                Map.of("codigo", saved.codigo(), "version", saved.version()),
                u.userId(), Instant.now(), "ANIMAL_ACTUALIZADO|" + id + "|" + saved.version()));
        audit(u, "ACTUALIZAR", id);
        return saved;
    }

    @Transactional
    public Animal changeState(UUID id, EstadoAnimal state, String reason, long version) {
        CurrentUser u = context.requirePermission(state == EstadoAnimal.ACTIVO ? "ANIMAL_CAMBIAR_ESTADO"
                : "ANIMAL_REGISTRAR_BAJA");
        Animal old = require(id, u.empresaId());
        context.requirePropertyAccess(u, old.propiedadActualId());
        if (old.estado() == state) return old;
        if (old.estado() == EstadoAnimal.MUERTO && state == EstadoAnimal.ACTIVO) {
            throw new BusinessException(ErrorCode.INVALID_ANIMAL_STATE_TRANSITION);
        }
        Animal saved = animals.changeState(id, u.empresaId(), old.estado(), state, reason, version, u.userId());
        timeline.publish(new RegistrarEventoTimeline(u.empresaId(), id, TipoEventoAnimal.ESTADO_CAMBIADO,
                null, reason, null, id,
                Map.of("estadoAnterior", old.estado().name(), "estadoNuevo", saved.estado().name(),
                        "motivo", reason, "version", saved.version()),
                u.userId(), Instant.now(), "ESTADO_CAMBIADO|" + id + "|" + saved.version()));
        audit(u, "CAMBIAR_ESTADO", id);
        return saved;
    }

    /**
     * Sincroniza la fotografía principal del animal (columna denormalizada)
     * cuando el módulo de archivos cambia la principal (Tarea 9.4).
     *
     * <p>La autorización de subida/marcado la aplica el módulo archivos; aquí
     * solo se valida que el animal pertenezca a la empresa y a las propiedades
     * permitidas del usuario.</p>
     */
    @Transactional
    public void asignarFotoPrincipal(UUID animalId, String path) {
        CurrentUser u = context.currentUser();
        Animal a = require(animalId, u.empresaId());
        context.requirePropertyAccess(u, a.propiedadActualId());
        animals.updateFotoPrincipal(animalId, u.empresaId(), path, u.userId());
    }

    @Transactional
    public void limpiarFotoPrincipal(UUID animalId) {
        CurrentUser u = context.currentUser();
        Animal a = require(animalId, u.empresaId());
        context.requirePropertyAccess(u, a.propiedadActualId());
        animals.updateFotoPrincipal(animalId, u.empresaId(), null, u.userId());
    }

    private void validateReferences(UUID breed, UUID category, SexoAnimal sex, UUID property, UUID paddock,
                                    CurrentUser u) {
        context.requirePropertyAccess(u, property);
        if (breeds.findById(breed, u.empresaId()).isEmpty()) throw new BusinessException(ErrorCode.BREED_NOT_FOUND);
        CategoriaAnimal cat = categories.findById(category, u.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_CATEGORY_NOT_FOUND));
        if (!cat.appliesTo(sex)) throw new BusinessException(ErrorCode.ANIMAL_CATEGORY_SEX_MISMATCH);
        if (!animals.validLocation(u.empresaId(), property, paddock)) {
            throw new BusinessException(ErrorCode.INVALID_ANIMAL_LOCATION);
        }
    }

    private Animal require(UUID id, UUID e) {
        return animals.findById(id, e).orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private void audit(CurrentUser u, String action, UUID id) {
        events.publishEvent(new AnimalAuditEvent(u.empresaId(), u.userId(), action, "ANIMAL", id, Instant.now()));
    }
}
