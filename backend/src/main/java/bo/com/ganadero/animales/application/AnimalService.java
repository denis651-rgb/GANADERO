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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnimalService {
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");
    private final AnimalRepository animals;
    private final RazaRepository breeds;
    private final CategoriaAnimalRepository categories;
    private final HistorialCategoriaAnimalRepository historial;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;
    private final TimelineService timelineService;
    private final CodigoService codigos;

    public AnimalService(AnimalRepository a, RazaRepository b, CategoriaAnimalRepository c,
                         HistorialCategoriaAnimalRepository historial, UserContext u,
                         ApplicationEventPublisher e, TimelineEventPublisher timeline,
                         TimelineService timelineService, CodigoService codigos) {
        animals = a;
        breeds = b;
        categories = c;
        this.historial = historial;
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
    public List<HistorialCategoriaAnimal> historialCategorias(UUID id) {
        CurrentUser u = context.requirePermission("ANIMAL_VER");
        Animal a = require(id, u.empresaId());
        context.requirePropertyAccess(u, a.propiedadActualId());
        return historial.listar(id);
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
        LocalDate fechaIngreso = fechaIngresoSegunOrigen(c.origen(), c.fechaNacimiento(), c.fechaIngreso());
        CategoriaResuelta categoriaResuelta = categoriaSegunEdad(c.sexo(), c.fechaNacimiento(), c.categoriaActualId(), u);
        UUID categoria = categoriaResuelta.id();
        validateEntryWeight(c.pesoIngresoKg(), c.pesoIngresoEstimado());
        validateReferences(c.razaPrincipalId(), categoria, c.sexo(), c.propiedadActualId(),
                c.potreroActualId(), u);
        UUID id = c.id() != null ? c.id() : UUID.randomUUID();
        String codigo = codigos.paraCreacion(u, TipoCodigo.ANIMAL, null, null, c.codigo());
        boolean estimada = c.fechaNacimiento() != null && Boolean.TRUE.equals(c.fechaNacimientoEstimada());
        Animal a = new Animal(id, u.empresaId(), codigo, c.nombre(), c.sexo(), c.fechaNacimiento(),
                estimada, c.razaPrincipalId(), categoria,
                c.color(), c.proposito(), c.origen(), c.propiedadActualId(), c.potreroActualId(), null,
                EstadoAnimal.ACTIVO, fechaIngreso,
                c.precioAdquisicion(), c.pesoNacimientoKg(), c.condicionCorporalActual(), c.fotoPrincipalPath(),
                c.observaciones(), 0, c.pesoIngresoKg(), c.pesoIngresoKg() == null ? null : c.pesoIngresoEstimado(),
                c.edadDeclaradaValor(), c.edadDeclaradaUnidad(), c.fechaReferenciaEdad(), c.fuenteEdad(),
                c.observacionEstimacion());
        Animal saved = animals.create(a, u.userId());
        registrarHistorialCategoria(saved.id(), null, categoria,
                categoriaResuelta.manual() ? HistorialCategoriaAnimal.MANUAL : HistorialCategoriaAnimal.AUTOMATICO,
                motivoCategoria(c.categoriaManualMotivo(), categoriaResuelta.manual(), saved.fechaNacimiento() == null),
                u.userId(), saved.fechaNacimiento(), saved.fechaNacimiento() != null && !saved.fechaNacimientoEstimada(),
                categoriaResuelta.manual() ? null : categoria);
        TipoEventoAnimal tipo = switch (a.origen()) {
            case NACIDO -> TipoEventoAnimal.NACIMIENTO_REGISTRADO;
            case COMPRADO -> TipoEventoAnimal.COMPRA_REGISTRADA;
            case TRANSFERIDO -> TipoEventoAnimal.INGRESO_REGISTRADO;
        };
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codigo", saved.codigo());
        metadata.put("sexo", saved.sexo().name());
        addEntryWeightMetadata(metadata, saved);
        if (saved.propiedadActualId() != null) metadata.put("propiedadId", saved.propiedadActualId().toString());
        timeline.publish(new RegistrarEventoTimeline(u.empresaId(), saved.id(), tipo, null, "Registro inicial",
                null, saved.id(), metadata, u.userId(), Instant.now(), null));
        audit(u, "CREAR", saved.id());
        return saved;
    }

    /**
     * Alta masiva (ingreso por lote de compra): reusa create() para cada comando dentro de
     * la MISMA transaccion. Si un animal falla (ej. codigo duplicado), toda la transaccion
     * hace rollback — mejor que el usuario corrija y reintente el lote completo a que
     * queden animales sueltos sin el resto de sus companeros de compra.
     */
    @Transactional
    public List<Animal> createBatch(List<AnimalCommand> comandos) {
        return comandos.stream().map(this::create).toList();
    }

    @Transactional
    public Animal update(UUID id, AnimalCommand c) {
        CurrentUser u = context.requirePermission("ANIMAL_EDITAR");
        Animal old = require(id, u.empresaId());
        context.requirePropertyAccess(u, old.propiedadActualId());
        boolean corregirPeso = Boolean.TRUE.equals(c.corregirPesoCompra());
        boolean quitarNacimiento = Boolean.TRUE.equals(c.quitarFechaNacimiento());
        boolean confirmarNacimiento = Boolean.TRUE.equals(c.confirmarFechaNacimiento());
        if (corregirPeso && (old.origen() != OrigenAnimal.COMPRADO || old.pesoNacimientoKg() == null
                || old.pesoIngresoKg() != null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Este animal no tiene un peso de compra pendiente de revisión.");
        }
        if (corregirPeso && (c.pesoIngresoKg() == null || c.pesoIngresoEstimado() == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Indica el peso al ingreso y si fue medido o estimado para confirmar la corrección.");
        }
        var pesoIngreso = c.pesoIngresoKg() == null ? old.pesoIngresoKg() : c.pesoIngresoKg();
        Boolean pesoEstimado = c.pesoIngresoEstimado() == null ? old.pesoIngresoEstimado() : c.pesoIngresoEstimado();
        validateEntryWeight(pesoIngreso, pesoEstimado);
        UUID property = c.propiedadActualId() == null ? old.propiedadActualId() : c.propiedadActualId();
        UUID paddock = c.potreroActualId() == null ? old.potreroActualId() : c.potreroActualId();
        boolean moves = !property.equals(old.propiedadActualId()) || !paddock.equals(old.potreroActualId());
        if (moves && old.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        SexoAnimal sex = c.sexo() == null ? old.sexo() : c.sexo();
        UUID breed = c.razaPrincipalId() == null ? old.razaPrincipalId() : c.razaPrincipalId();
        LocalDate nacimiento = quitarNacimiento ? null : c.fechaNacimiento() == null ? old.fechaNacimiento() : c.fechaNacimiento();
        UUID categoriaSolicitada = c.categoriaActualId() == null ? old.categoriaActualId() : c.categoriaActualId();
        CategoriaResuelta categoriaResuelta = categoriaSegunEdad(sex, nacimiento, categoriaSolicitada, u);
        UUID category = categoriaResuelta.id();
        LocalDate fechaIngreso = fechaIngresoSegunOrigen(old.origen(), nacimiento,
                c.fechaIngreso() == null ? old.fechaIngreso() : c.fechaIngreso());
        validateReferences(breed, category, sex, property, paddock, u);
        String codigo = codigos.paraActualizacion(u, TipoCodigo.ANIMAL, null, null, old.codigo(), c.codigo());
        boolean limpiarEstimacion = quitarNacimiento || confirmarNacimiento;
        Animal value = new Animal(id, u.empresaId(), codigo, c.nombre(), sex, c.fechaNacimiento(),
                !quitarNacimiento && (c.fechaNacimiento() != null || old.fechaNacimiento() != null)
                        && (c.fechaNacimientoEstimada() == null ? old.fechaNacimientoEstimada() : c.fechaNacimientoEstimada()),
                breed, category, c.color(), c.proposito() == null ? old.proposito() : c.proposito(), old.origen(),
                property, paddock, old.loteActualId(), old.estado(), fechaIngreso, c.precioAdquisicion(),
                c.pesoNacimientoKg(), c.condicionCorporalActual(), c.fotoPrincipalPath(), c.observaciones(),
                Objects.requireNonNull(c.version()), pesoIngreso, pesoIngreso == null ? null : pesoEstimado,
                c.edadDeclaradaValor(), c.edadDeclaradaUnidad(), c.fechaReferenciaEdad(), c.fuenteEdad(),
                c.observacionEstimacion());
        Animal saved = (quitarNacimiento || corregirPeso || limpiarEstimacion)
                ? animals.update(value, u.userId(), quitarNacimiento, corregirPeso, limpiarEstimacion)
                : animals.update(value, u.userId());
        registrarCambioCategoria(old, saved, categoriaResuelta, c.categoriaManualMotivo(), u.userId());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("codigo", saved.codigo());
        metadata.put("version", saved.version());
        addEntryWeightMetadata(metadata, saved);
        if (corregirPeso) {
            metadata.put("correccionPesoCompraConfirmada", true);
            metadata.put("pesoNacimientoAnteriorKg", old.pesoNacimientoKg());
        }
        if (quitarNacimiento) metadata.put("nacimientoDesconocido", true);
        timeline.publish(new RegistrarEventoTimeline(u.empresaId(), id, TipoEventoAnimal.ANIMAL_ACTUALIZADO,
                null, corregirPeso ? "Peso de compra corregido: no corresponde al nacimiento" : "Datos del animal actualizados", null, id,
                metadata,
                u.userId(), Instant.now(), "ANIMAL_ACTUALIZADO|" + id + "|" + saved.version()));
        audit(u, "ACTUALIZAR", id);
        return saved;
    }

    private LocalDate fechaIngresoSegunOrigen(OrigenAnimal origen, LocalDate nacimiento, LocalDate ingresoSolicitado) {
        if (origen == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "El origen del animal es obligatorio.");
        LocalDate hoy = LocalDate.now(BOLIVIA);
        if (origen == OrigenAnimal.NACIDO) {
            if (nacimiento == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Un animal nacido en la finca debe tener fecha de nacimiento; esa misma fecha será su ingreso.");
            if (nacimiento.isAfter(hoy)) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "La fecha de nacimiento no puede estar en el futuro.");
            return nacimiento;
        }
        if (ingresoSolicitado == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La fecha de recepción es obligatoria para animales comprados o transferidos.");
        if (ingresoSolicitado.isAfter(hoy)) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La fecha de recepción no puede estar en el futuro.");
        if (nacimiento != null && ingresoSolicitado.isBefore(nacimiento)) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La fecha de recepción no puede ser anterior al nacimiento.");
        return ingresoSolicitado;
    }

    private void validateEntryWeight(java.math.BigDecimal peso, Boolean estimado) {
        if (peso != null && (peso.signum() <= 0 || estimado == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "El peso al ingreso debe ser positivo e indicar si fue medido o estimado.");
        }
    }

    private void addEntryWeightMetadata(Map<String, Object> metadata, Animal animal) {
        if (animal.pesoIngresoKg() == null) return;
        metadata.put("pesoIngresoKg", animal.pesoIngresoKg());
        metadata.put("pesoIngresoEstimado", Boolean.TRUE.equals(animal.pesoIngresoEstimado()));
        if (animal.fechaIngreso() != null) metadata.put("fechaIngreso", animal.fechaIngreso().toString());
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

    /** Resultado de resolver la categoría: si vino del cálculo automático por edad o de una elección manual/excepción. */
    private record CategoriaResuelta(UUID id, boolean manual) {}

    private CategoriaResuelta categoriaSegunEdad(SexoAnimal sexo, LocalDate nacimiento, UUID categoriaSolicitada, CurrentUser u) {
        if (nacimiento == null) {
            // Edad desconocida: nunca se infiere; la categoría queda como elección manual explícita.
            if (categoriaSolicitada == null) throw new BusinessException(ErrorCode.ANIMAL_CATEGORY_NOT_FOUND);
            return new CategoriaResuelta(categoriaSolicitada, true);
        }
        LocalDate hoy = LocalDate.now(BOLIVIA);
        if (categoriaSolicitada != null) {
            Optional<CategoriaAnimal> manual = categories.findById(categoriaSolicitada, u.empresaId());
            if (manual.isPresent() && !manual.get().clasificacionAutomatica()
                    && manual.get().appliesTo(sexo, nacimiento, hoy)) return new CategoriaResuelta(categoriaSolicitada, true);
        }
        List<CategoriaAnimal> candidatas = categories.findActive(u.empresaId()).stream()
                .filter(CategoriaAnimal::clasificacionAutomatica)
                .filter(categoria -> categoria.appliesTo(sexo, nacimiento, hoy))
                .toList();
        if (candidatas.size() == 1) return new CategoriaResuelta(candidatas.getFirst().id(), false);
        // Compatibilidad con catálogos simulados/antiguos: la validación posterior sigue verificando sexo.
        if (candidatas.isEmpty() && categoriaSolicitada != null) return new CategoriaResuelta(categoriaSolicitada, true);
        throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "No existe una categoría automática única para el sexo y la edad del animal. Revisa los rangos del catálogo.");
    }

    private String motivoCategoria(String motivoDeclarado, boolean manual, boolean edadDesconocida) {
        if (motivoDeclarado != null && !motivoDeclarado.isBlank()) return motivoDeclarado.trim();
        if (!manual) return "Asignación automática inicial por sexo y edad.";
        return edadDesconocida ? "Edad desconocida: categoría seleccionada manualmente." : "Categoría manual asignada al registrar el animal.";
    }

    private void registrarCambioCategoria(Animal old, Animal saved, CategoriaResuelta categoriaResuelta,
                                          String motivoDeclarado, UUID actor) {
        boolean nacimientoCambio = !Objects.equals(saved.fechaNacimiento(), old.fechaNacimiento());
        boolean eraDesconocida = old.fechaNacimiento() == null;
        boolean esCorreccion = nacimientoCambio || (eraDesconocida && saved.fechaNacimiento() != null);
        String tipo = esCorreccion ? HistorialCategoriaAnimal.CORRECCION
                : categoriaResuelta.manual() ? HistorialCategoriaAnimal.MANUAL : HistorialCategoriaAnimal.AUTOMATICO;
        boolean categoriaCambio = !Objects.equals(old.categoriaActualId(), saved.categoriaActualId());
        if (!categoriaCambio && !esCorreccion) return;
        String motivo = motivoDeclarado != null && !motivoDeclarado.isBlank() ? motivoDeclarado.trim()
                : esCorreccion ? "Corrección de fecha de nacimiento o edad declarada."
                : categoriaResuelta.manual() ? "Categoría asignada manualmente." : "Reclasificación automática por sexo y edad.";
        registrarHistorialCategoria(saved.id(), old.categoriaActualId(), saved.categoriaActualId(), tipo, motivo,
                actor, saved.fechaNacimiento(), saved.fechaNacimiento() != null && !saved.fechaNacimientoEstimada(),
                categoriaResuelta.manual() ? null : saved.categoriaActualId());
    }

    private void registrarHistorialCategoria(UUID animalId, UUID categoriaAnterior, UUID categoriaNueva, String tipo,
                                             String motivo, UUID usuarioId, LocalDate nacimiento, boolean confirmada,
                                             UUID categoriaConfigId) {
        Long edadDias = nacimiento == null ? null : ChronoUnit.DAYS.between(nacimiento, LocalDate.now(BOLIVIA));
        historial.crear(new HistorialCategoriaAnimal(UUID.randomUUID(), animalId, categoriaAnterior, categoriaNueva,
                Instant.now(), tipo, motivo, usuarioId, edadDias, confirmada, categoriaConfigId));
    }

    private Animal require(UUID id, UUID e) {
        return animals.findById(id, e).orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private void audit(CurrentUser u, String action, UUID id) {
        events.publishEvent(new AnimalAuditEvent(u.empresaId(), u.userId(), action, "ANIMAL", id, Instant.now()));
    }
}
