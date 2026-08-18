package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.reproduccion.domain.Celo;
import bo.com.ganadero.reproduccion.domain.CeloPage;
import bo.com.ganadero.reproduccion.domain.DiagnosticoGestacion;
import bo.com.ganadero.reproduccion.domain.DiagnosticoPage;
import bo.com.ganadero.reproduccion.domain.EstadoRegistroReproduccion;
import bo.com.ganadero.reproduccion.domain.EstadoServicio;
import bo.com.ganadero.reproduccion.domain.ReproduccionAnimal;
import bo.com.ganadero.reproduccion.domain.ReproduccionRepository;
import bo.com.ganadero.reproduccion.domain.ResultadoGestacion;
import bo.com.ganadero.reproduccion.domain.Servicio;
import bo.com.ganadero.reproduccion.domain.ServicioPage;
import bo.com.ganadero.reproduccion.domain.TipoCelo;
import bo.com.ganadero.reproduccion.domain.TipoServicio;
import bo.com.ganadero.shared.audit.AuditActions;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import bo.com.ganadero.alertas.application.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReproduccionService {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");
    private final ReproduccionRepository registros;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;
    private ObjectProvider<MotorAlertas> alertas;
    private ObjectProvider<AlertaConfiguracionPort> configuracionAlertas;

    public ReproduccionService(ReproduccionRepository registros, AnimalRepository animales,
                               UserContext context, ApplicationEventPublisher events,
                               TimelineEventPublisher timeline) {
        this.registros = registros;
        this.animales = animales;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
    }

    @Autowired
    public ReproduccionService(ReproduccionRepository registros, AnimalRepository animales, UserContext context,
            ApplicationEventPublisher events, TimelineEventPublisher timeline, ObjectProvider<MotorAlertas> alertas,
            ObjectProvider<AlertaConfiguracionPort> configuracionAlertas) {
        this(registros, animales, context, events, timeline); this.alertas=alertas; this.configuracionAlertas=configuracionAlertas;
    }

    @Transactional(readOnly = true)
    public CeloPage listCelos(UUID animalId, Instant fechaDesde, Instant fechaHasta,
                              bo.com.ganadero.reproduccion.domain.IntensidadCelo intensidad,
                              EstadoRegistroReproduccion estado, UUID propiedadId, int page, int size) {
        CurrentUser user = context.requirePermission("REPRODUCCION_VER");
        if (animalId != null) requireAnimal(user, animalId);
        if (propiedadId != null) context.requirePropertyAccess(user, propiedadId);
        return registros.findAllCelos(user.empresaId(), user.propiedadesPermitidas(),
                user.accesoTodasPropiedades(), animalId, fechaDesde, fechaHasta, intensidad, estado,
                propiedadId, page, size);
    }

    @Transactional(readOnly = true)
    public Celo getCelo(UUID id) {
        CurrentUser user = context.requirePermission("REPRODUCCION_VER");
        Celo celo = registros.findCeloById(id, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        context.requirePropertyAccess(user, celo.propiedadId());
        return celo;
    }

    @Transactional
    public Celo anularCelo(UUID id, String motivo, long version) {
        CurrentUser user = context.requirePermission("REPRODUCCION_ANULAR");
        if (motivo == null || motivo.isBlank()) throw new BusinessException(ErrorCode.REPRODUCCION_MOTIVO_REQUERIDO);
        Celo actual = getCeloForUser(user, id);
        if (actual.estado() == EstadoRegistroReproduccion.ANULADO) {
            throw new BusinessException(ErrorCode.REPRODUCCION_YA_ANULADO);
        }
        Celo saved = registros.annulCelo(id, user.empresaId(), motivo, version, user.userId());
        motor().ifPresent(m -> m.cancelarPorOrigen(user.empresaId(), "CELO", saved.id(), "CELO_ANULADO"));
        audit(user, AuditActions.ANULAR_CELO, saved.id());
        return saved;
    }

    private Celo getCeloForUser(CurrentUser user, UUID id) {
        Celo celo = registros.findCeloById(id, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        context.requirePropertyAccess(user, celo.propiedadId());
        return celo;
    }

    @Transactional
    public Celo registrarCelo(RegistrarCeloCommand command) {
        CurrentUser user = context.requirePermission("REPRODUCCION_REGISTRAR");
        Instant fecha = validarFecha(command.fechaDeteccion());
        Animal animal = requireHembraActiva(user, command.animalId());

        UUID property = comandoPropiedad(command.propiedadId(), animal);
        context.requirePropertyAccess(user, property);

        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        UUID clienteUuid = command.clienteUuid() != null ? command.clienteUuid() : id;
        Celo value = new Celo(id, user.empresaId(), command.animalId(), fecha,
                command.tipoDeteccion(), command.intensidad(), user.userId(), command.observaciones(), property,
                command.potreroId() != null ? command.potreroId() : animal.potreroActualId(),
                command.loteId() != null ? command.loteId() : animal.loteActualId(),
                clienteUuid, command.idempotencyKey(), EstadoRegistroReproduccion.ACTIVO, null, null, null,
                null, null, null, null, 0);
        Celo saved = registros.createCelo(value, user.userId());
        motor().ifPresent(m -> m.crearInmediata(new ProgramarAlertaCommand(user.empresaId(), saved.animalId(),
                TipoAlerta.CELO_DETECTADO, Instant.now(), "CELO", saved.id(),
                metadataAnimal(animal, Map.of("fechaDeteccion", saved.fechaDeteccion().toString())))));
        publicarCelo(user, saved);
        audit(user, AuditActions.REGISTRAR_CELO, saved.id());
        return saved;
    }

    @Transactional(readOnly = true)
    public ServicioPage listServicios(UUID animalId, UUID propiedadId, int page, int size) {
        CurrentUser user = context.requirePermission("REPRODUCCION_VER");
        if (animalId != null) requireAnimal(user, animalId);
        if (propiedadId != null) context.requirePropertyAccess(user, propiedadId);
        return registros.findAllServicios(user.empresaId(), user.propiedadesPermitidas(),
                user.accesoTodasPropiedades(), animalId, propiedadId, page, size);
    }

    @Transactional
    public Servicio registrarServicio(RegistrarServicioCommand command) {
        CurrentUser user = context.requirePermission("REPRODUCCION_REGISTRAR");
        Instant fecha = validarFecha(command.fechaServicio());
        Animal animal = requireHembraActiva(user, command.hembraId());
        validarPosteriorAlNacimiento(fecha, animal);

        UUID property = comandoPropiedad(command.propiedadId(), animal);
        context.requirePropertyAccess(user, property);

        Animal macho = null;
        if (command.tipoServicio() == TipoServicio.MONTA_NATURAL) {
            if (command.machoId() != null && command.machoId().equals(animal.id())) {
                throw new BusinessException(ErrorCode.SERVICIO_MACHO_IGUAL);
            }
            macho = requireMacho(user, command.machoId());
        }
        if (command.celoId() != null) {
            Celo celo = registros.findCeloById(command.celoId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
            if (!celo.animalId().equals(animal.id())) {
                throw new BusinessException(ErrorCode.SERVICIO_CELO_INCOMPATIBLE);
            }
            if (celo.estado() == EstadoRegistroReproduccion.ANULADO) {
                throw new BusinessException(ErrorCode.SERVICIO_CELO_INCOMPATIBLE);
            }
        }

        int numeroIntento = registros.countServicios(animal.id(), user.empresaId()) + 1;
        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        UUID clienteUuid = command.clienteUuid() != null ? command.clienteUuid() : id;
        AlertaConfiguracion configuracion = configuracion(user.empresaId());
        Servicio value = new Servicio(id, user.empresaId(), command.hembraId(), command.celoId(),
                fecha, command.tipoServicio(), command.machoId(), command.codigoSemen(), command.proveedorSemen(),
                command.tecnicoId(), numeroIntento,
                fecha.plusSeconds(configuracion.diasDiagnosticoPostServicio() * 86400L), command.observaciones(),
                property,
                command.potreroId() != null ? command.potreroId() : animal.potreroActualId(),
                command.loteId() != null ? command.loteId() : animal.loteActualId(),
                clienteUuid, command.idempotencyKey(), EstadoServicio.PENDIENTE_DIAGNOSTICO, null, null, null,
                null, null, null, null, null, null, 0);
        Servicio saved = registros.createServicio(value, user.userId());
        if (saved.celoId() != null) {
            motor().ifPresent(m -> m.resolverPorOrigen(user.empresaId(), "CELO", saved.celoId()));
        }
        motor().ifPresent(m -> m.programar(new ProgramarAlertaCommand(user.empresaId(), saved.hembraId(),
                TipoAlerta.DIAGNOSTICO_PENDIENTE, saved.fechaDiagnosticoRecomendada(), "SERVICIO", saved.id(),
                metadataAnimal(animal, Map.of("fechaDiagnostico", saved.fechaDiagnosticoRecomendada().toString())))));
        publicarServicio(user, saved);
        audit(user, AuditActions.REGISTRAR_SERVICIO, saved.id());
        return saved;
    }

    @Transactional(readOnly = true)
    public DiagnosticoPage listDiagnosticos(UUID animalId, UUID propiedadId, int page, int size) {
        CurrentUser user = context.requirePermission("REPRODUCCION_VER");
        if (animalId != null) requireAnimal(user, animalId);
        if (propiedadId != null) context.requirePropertyAccess(user, propiedadId);
        return registros.findAllDiagnosticos(user.empresaId(), user.propiedadesPermitidas(),
                user.accesoTodasPropiedades(), animalId, propiedadId, page, size);
    }

    @Transactional
    public DiagnosticoGestacion registrarDiagnostico(RegistrarDiagnosticoCommand command) {
        CurrentUser user = context.requirePermission("REPRODUCCION_REGISTRAR");
        Instant fecha = validarFecha(command.fechaDiagnostico());
        Animal animal = requireHembraActiva(user, command.animalId());

        UUID property = comandoPropiedad(command.propiedadId(), animal);
        context.requirePropertyAccess(user, property);

        Servicio servicio = null;
        if (command.servicioId() != null) {
            servicio = registros.findServicioById(command.servicioId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
            if (!servicio.hembraId().equals(animal.id()) || servicio.estado() == EstadoServicio.ANULADO) {
                throw new BusinessException(ErrorCode.DIAGNOSTICO_SERVICIO_INCOMPATIBLE);
            }
            if (!fecha.isAfter(servicio.fechaServicio())) {
                throw new BusinessException(ErrorCode.DIAGNOSTICO_FECHA_ANTERIOR_SERVICIO);
            }
        }

        LocalDate fechaProbableParto = null;
        if (command.resultado() == ResultadoGestacion.POSITIVO) {
            AlertaConfiguracion configuracion = configuracion(user.empresaId());
            if (command.diasGestacionEstimados() != null) {
                if (command.diasGestacionEstimados() > configuracion.diasGestacionEstimada()) {
                    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                            "Los días de gestación estimados superan la duración configurada para la empresa.");
                }
                int diasRestantes = configuracion.diasGestacionEstimada() - command.diasGestacionEstimados();
                fechaProbableParto = fecha.atZone(ZoneOffset.UTC).toLocalDate().plusDays(diasRestantes);
            } else {
                LocalDate base = (servicio != null ? servicio.fechaServicio() : fecha)
                        .atZone(ZoneOffset.UTC).toLocalDate();
                fechaProbableParto = base.plusDays(configuracion.diasGestacionEstimada());
            }
        }

        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        UUID clienteUuid = command.clienteUuid() != null ? command.clienteUuid() : id;
        DiagnosticoGestacion value = new DiagnosticoGestacion(id, user.empresaId(), command.animalId(),
                command.servicioId(), fecha, command.resultado(), command.metodo(), command.diasGestacionEstimados(),
                fechaProbableParto, command.veterinarioId(), command.observaciones(), property,
                command.potreroId() != null ? command.potreroId() : animal.potreroActualId(),
                command.loteId() != null ? command.loteId() : animal.loteActualId(),
                clienteUuid, command.idempotencyKey(), EstadoRegistroReproduccion.ACTIVO,
                null, null, null, null, 0);
        DiagnosticoGestacion saved = registros.createDiagnostico(value, user.userId());
        UUID servicioDiagnosticadoId = servicio == null ? null : servicio.id();
        if (servicioDiagnosticadoId != null) motor().ifPresent(m -> m.resolverPorOrigen(user.empresaId(), "SERVICIO", servicioDiagnosticadoId));
        if (saved.resultado() == ResultadoGestacion.POSITIVO && saved.fechaProbableParto() != null) {
            int dias = configuracion(user.empresaId()).diasAlertaPreparto();
            Instant objetivo=saved.fechaProbableParto().atStartOfDay(ZONA_NEGOCIO).toInstant();
            motor().ifPresent(m -> m.programar(new ProgramarAlertaCommand(user.empresaId(),saved.animalId(),TipoAlerta.PARTO_PROXIMO,
                    objetivo.minusSeconds(dias*86400L),objetivo,"GESTACION",saved.id(),
                    metadataAnimal(animal, Map.of("fechaProbableParto", objetivo.toString())))));
        }
        if (servicio != null) {
            EstadoServicio estado = switch (saved.resultado()) {
                case POSITIVO -> EstadoServicio.GESTACION_CONFIRMADA;
                case NEGATIVO -> EstadoServicio.NO_PRENADA;
                case DUDOSO -> EstadoServicio.PENDIENTE_DIAGNOSTICO;
                case PERDIDA_GESTACION -> EstadoServicio.FINALIZADO;
            };
            registros.updateServicioEstado(servicio.id(), user.empresaId(), estado, user.userId());
        }
        publicarDiagnostico(user, saved);
        audit(user, AuditActions.REGISTRAR_DIAGNOSTICO, saved.id());
        return saved;
    }

    @Transactional(readOnly = true)
    public ReproduccionAnimal reproduccionAnimal(UUID animalId) {
        CurrentUser user = context.requirePermission("REPRODUCCION_VER");
        requireAnimal(user, animalId);
        return new ReproduccionAnimal(animalId,
                registros.celosDeAnimal(animalId, user.empresaId()),
                registros.serviciosDeAnimal(animalId, user.empresaId()),
                registros.diagnosticosDeAnimal(animalId, user.empresaId()));
    }

    private Instant validarFecha(Instant fecha) {
        Instant value = fecha == null ? Instant.now() : fecha;
        if (value.isAfter(Instant.now())) throw new BusinessException(ErrorCode.REPRODUCCION_FECHA_INVALIDA);
        return value;
    }

    private void validarPosteriorAlNacimiento(Instant fecha, Animal animal) {
        if (animal.fechaNacimiento() != null && !fecha.atZone(ZoneOffset.UTC).toLocalDate().isAfter(animal.fechaNacimiento())) {
            throw new BusinessException(ErrorCode.REPRODUCCION_FECHA_ANTERIOR_NACIMIENTO);
        }
    }

    private UUID comandoPropiedad(UUID propiedadId, Animal animal) {
        UUID property = propiedadId != null ? propiedadId : animal.propiedadActualId();
        if (!property.equals(animal.propiedadActualId())) {
            throw new BusinessException(ErrorCode.ANIMAL_PROPERTY_MISMATCH);
        }
        return property;
    }

    private Animal requireHembraActiva(CurrentUser user, UUID animalId) {
        Animal animal = requireAnimal(user, animalId);
        if (animal.sexo() != SexoAnimal.HEMBRA) {
            throw new BusinessException(ErrorCode.REPRODUCCION_SOLO_HEMBRA);
        }
        if (animal.estado() != EstadoAnimal.ACTIVO) {
            throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        }
        return animal;
    }

    private Animal requireMacho(CurrentUser user, UUID machoId) {
        if (machoId == null) throw new BusinessException(ErrorCode.SERVICIO_MACHO_INVALIDO);
        Animal macho = animales.findById(machoId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        if (macho.sexo() != SexoAnimal.MACHO || macho.estado() != EstadoAnimal.ACTIVO) {
            throw new BusinessException(ErrorCode.SERVICIO_MACHO_INVALIDO);
        }
        return macho;
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private void publicarCelo(CurrentUser user, Celo celo) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fechaDeteccion", celo.fechaDeteccion().toString());
        metadata.put("tipoDeteccion", celo.tipoDeteccion().name());
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), celo.animalId(),
                TipoEventoAnimal.CELO_DETECTADO, null, "Celo detectado · " + celo.tipoDeteccion().name(),
                null, celo.id(), metadata, user.userId(), Instant.now(), null));
    }

    private void publicarServicio(CurrentUser user, Servicio servicio) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fechaServicio", servicio.fechaServicio().toString());
        metadata.put("tipo", servicio.tipoServicio().name());
        metadata.put("numeroIntento", servicio.numeroIntento());
        if (servicio.machoId() != null) metadata.put("machoId", servicio.machoId());
        if (servicio.celoId() != null) metadata.put("celoId", servicio.celoId());
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), servicio.hembraId(),
                TipoEventoAnimal.SERVICIO_REGISTRADO, null,
                "Servicio " + servicio.tipoServicio().name() + " · intento " + servicio.numeroIntento(),
                null, servicio.id(), metadata, user.userId(), Instant.now(), null));
    }

    private void publicarDiagnostico(CurrentUser user, DiagnosticoGestacion diagnostico) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fechaDiagnostico", diagnostico.fechaDiagnostico().toString());
        metadata.put("resultado", diagnostico.resultado().name());
        if (diagnostico.metodo() != null) metadata.put("metodo", diagnostico.metodo().name());
        if (diagnostico.fechaProbableParto() != null) {
            metadata.put("fechaProbableParto", diagnostico.fechaProbableParto().toString());
        }
        TipoEventoAnimal tipo = switch (diagnostico.resultado()) {
            case POSITIVO -> TipoEventoAnimal.GESTACION_CONFIRMADA;
            case NEGATIVO -> TipoEventoAnimal.GESTACION_DESCARTADA;
            case DUDOSO -> TipoEventoAnimal.DIAGNOSTICO_GESTACION_REGISTRADO;
            case PERDIDA_GESTACION -> TipoEventoAnimal.PERDIDA_GESTACION;
        };
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), diagnostico.animalId(),
                tipo, null, "Diagnóstico de gestación " + diagnostico.resultado().name(),
                null, diagnostico.id(), metadata, user.userId(), Instant.now(), null));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new ReproduccionAuditEvent(user.empresaId(), user.userId(), accion, "REPRODUCCION", id, Instant.now()));
    }
    private java.util.Optional<MotorAlertas> motor(){return alertas==null?java.util.Optional.empty():java.util.Optional.ofNullable(alertas.getIfAvailable());}
    private AlertaConfiguracion configuracion(UUID empresaId) {
        if (configuracionAlertas == null) return AlertaConfiguracion.valoresPredeterminados();
        AlertaConfiguracionPort port = configuracionAlertas.getIfAvailable();
        return port == null ? AlertaConfiguracion.valoresPredeterminados() : port.obtener(empresaId);
    }
    private Map<String, Object> metadataAnimal(Animal animal, Map<String, Object> adicionales) {
        Map<String, Object> metadata = new HashMap<>(adicionales);
        metadata.put("animalCodigo", animal.codigo());
        if (animal.nombre() != null && !animal.nombre().isBlank()) metadata.put("animalNombre", animal.nombre());
        return metadata;
    }
}
