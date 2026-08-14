package bo.com.ganadero.lotes.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoteService {
    private final LoteRepository lotes;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;
    private final CodigoService codigos;

    public LoteService(LoteRepository lotes, AnimalRepository animales, UserContext context,
                       ApplicationEventPublisher events, TimelineEventPublisher timeline, CodigoService codigos) {
        this.lotes = lotes;
        this.animales = animales;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
        this.codigos = codigos;
    }

    @Transactional(readOnly = true)
    public LotePage list(EstadoLote estado, String search, int page, int size) {
        CurrentUser user = context.requirePermission("LOTE_VER");
        return lotes.findAll(user.empresaId(), user.propiedadesPermitidas(), user.accesoTodasPropiedades(),
                estado, search, page, size);
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

    @Transactional(readOnly = true)
    public MembresiaLotePage historial(UUID id, HistorialLoteFilter filter) {
        CurrentUser user = context.requirePermission("LOTE_VER");
        require(id, user.empresaId());
        return lotes.findHistory(id, user.empresaId(), filter.animalId(), filter.desde(), filter.hasta(),
                filter.motivoIngreso(), filter.motivoSalida(), filter.page(), filter.size());
    }

    @Transactional
    public Lote create(LoteCommand command) {
        CurrentUser user = context.requirePermission("LOTE_CREAR");
        context.requirePropertyAccess(user, command.propiedadId());
        LocalDate apertura = command.fechaApertura() == null ? LocalDate.now() : command.fechaApertura();
        String codigo = codigos.paraCreacion(user, TipoCodigo.LOTE, null, apertura.getYear(), command.codigo());
        Lote value = new Lote(UUID.randomUUID(), user.empresaId(), command.propiedadId(), codigo,
                command.nombre(), command.descripcion(), EstadoLote.ACTIVO,
                apertura, null, 0);
        Lote saved = lotes.create(value, user.userId());
        audit(user, "CREAR_LOTE", saved.id());
        return saved;
    }

    @Transactional
    public Lote update(UUID id, LoteCommand command) {
        CurrentUser user = context.requirePermission("LOTE_EDITAR");
        Lote old = require(id, user.empresaId());
        if (old.estado() == EstadoLote.CERRADO) throw new BusinessException(ErrorCode.LOT_ALREADY_CLOSED);
        UUID property = command.propiedadId() == null ? old.propiedadId() : command.propiedadId();
        context.requirePropertyAccess(user, property);
        String codigo = codigos.paraActualizacion(user, TipoCodigo.LOTE, null, old.fechaApertura().getYear(),
                old.codigo(), command.codigo());
        Lote value = new Lote(id, user.empresaId(), property,
                codigo,
                command.nombre() == null ? old.nombre() : command.nombre(),
                command.descripcion() == null ? old.descripcion() : command.descripcion(),
                old.estado(), old.fechaApertura(), old.fechaCierre(), old.version());
        Lote saved = lotes.update(value, user.userId());
        audit(user, "ACTUALIZAR_LOTE", saved.id());
        return saved;
    }

    @Transactional
    public Lote close(UUID id, long version, LocalDate fechaCierre, String motivoCierre) {
        CurrentUser user = context.requirePermission("LOTE_EDITAR");
        Lote lote = require(id, user.empresaId());
        if (lote.estado() == EstadoLote.CERRADO) throw new BusinessException(ErrorCode.LOT_ALREADY_CLOSED);
        if (lotes.hasActiveAnimals(id, user.empresaId())) {
            throw new BusinessException(ErrorCode.LOT_HAS_ACTIVE_ANIMALS);
        }
        Lote saved = lotes.close(id, user.empresaId(), version, fechaCierre, motivoCierre, user.userId());
        audit(user, "CERRAR_LOTE", saved.id());
        return saved;
    }

    @Transactional
    public IngresoMasivoResultado addAnimals(UUID loteId, IngresoLoteCommand command) {
        CurrentUser user = context.requirePermission("LOTE_ASIGNAR_ANIMALES");
        Lote lote = require(loteId, user.empresaId());
        context.requirePropertyAccess(user, lote.propiedadId());
        if (lote.estado() != EstadoLote.ACTIVO) throw new BusinessException(ErrorCode.LOT_CLOSED);
        List<UUID> ids = requireUnique(command.animalIds());
        ModoIngreso modo = command.modo() == null ? ModoIngreso.PARCIAL : parseModo(command.modo());
        Instant fechaIngreso = command.fechaIngreso() == null ? Instant.now() : command.fechaIngreso();
        if (fechaIngreso.isAfter(Instant.now())) throw new BusinessException(ErrorCode.INVALID_MEMBERSHIP_DATE);
        List<ResultadoAccion> resultados = new ArrayList<>();
        int ingresados = 0;
        for (UUID animalId : ids) {
            try {
                ResultadoAccion resultado = ingresar(user, lote, animalId, fechaIngreso,
                        command.motivo(), command.observacion(), modo);
                resultados.add(resultado);
                if ("OK".equals(resultado.estado())) ingresados++;
            } catch (BusinessException ex) {
                if (modo == ModoIngreso.ATOMICO) throw ex;
                resultados.add(new ResultadoAccion(animalId, "ERROR", ex.getMessage()));
            }
        }
        boolean ok = resultados.stream().allMatch(r -> "OK".equals(r.estado()));
        audit(user, "INGRESO_MASIVO_LOTE", loteId);
        return new IngresoMasivoResultado(ok, ids.size(), ingresados, resultados);
    }

    @Transactional
    public RetiroMasivoResultado removeAnimals(UUID loteId, RetiroLoteCommand command) {
        CurrentUser user = context.requirePermission("LOTE_ASIGNAR_ANIMALES");
        Lote lote = require(loteId, user.empresaId());
        context.requirePropertyAccess(user, lote.propiedadId());
        List<UUID> ids = requireUnique(command.animalIds());
        Instant fechaSalida = command.fechaSalida() == null ? Instant.now() : command.fechaSalida();
        List<ResultadoAccion> resultados = new ArrayList<>();
        int retirados = 0;
        for (UUID animalId : ids) {
            try {
                retirar(user, lote, animalId, fechaSalida, command.motivo());
                retirados++;
                resultados.add(new ResultadoAccion(animalId, "OK", "Animal retirado del lote"));
            } catch (BusinessException ex) {
                resultados.add(new ResultadoAccion(animalId, "ERROR", ex.getMessage()));
            }
        }
        boolean ok = resultados.stream().allMatch(r -> "OK".equals(r.estado()));
        audit(user, "RETIRO_MASIVO_LOTE", loteId);
        return new RetiroMasivoResultado(ok, ids.size(), retirados, resultados);
    }

    private ResultadoAccion ingresar(CurrentUser user, Lote lote, UUID animalId, Instant fechaIngreso,
                                     String motivo, String observacion, ModoIngreso modo) {
        Animal animal = requireAnimal(user, animalId);
        if (animal.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED);
        if (!lote.propiedadId().equals(animal.propiedadActualId())) {
            throw new BusinessException(ErrorCode.ANIMAL_PROPERTY_MISMATCH);
        }
        boolean cambio = false;
        Optional<MembresiaLote> actual = lotes.findActiveMembership(animalId, user.empresaId());
        if (actual.isPresent()) {
            if (actual.get().loteId().equals(lote.id())) throw new BusinessException(ErrorCode.ANIMAL_ALREADY_IN_LOT);
            Lote viejo = lotes.findById(actual.get().loteId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
            lotes.closeMembership(viejo.id(), viejo.codigo(), animalId, user.empresaId(), "CAMBIO_LOTE",
                    Instant.now(), user.userId());
            cambio = true;
        }
        MembresiaLote membresia = lotes.openMembership(lote.id(), lote.codigo(), animalId, user.empresaId(),
                motivo, observacion, modo.name(), fechaIngreso, user.userId());
        animales.updateLote(animalId, user.empresaId(), lote.id(), user.userId());
        if (cambio) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("loteAnteriorId", actual.get().loteId().toString());
            metadata.put("loteNuevoId", lote.id().toString());
            metadata.put("loteNuevoCodigo", lote.codigo());
            metadata.put("membresiaId", actual.get().id().toString());
            publicarLote(user, animalId, TipoEventoAnimal.LOTE_CAMBIADO, "Salida por cambio de lote",
                    actual.get().id(), metadata);
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.id().toString());
        metadata.put("loteCodigo", lote.codigo());
        metadata.put("membresiaId", membresia.id().toString());
        metadata.put("fechaIngreso", fechaIngreso.toString());
        publicarLote(user, animalId, TipoEventoAnimal.LOTE_ASIGNADO, "Ingreso al lote",
                membresia.id(), metadata);
        audit(user, cambio ? "CAMBIAR_ANIMAL_LOTE" : "INGRESAR_ANIMAL_LOTE", lote.id());
        return new ResultadoAccion(animalId, "OK", cambio ? "Animal movido al lote" : "Animal ingresado al lote");
    }

    private void retirar(CurrentUser user, Lote lote, UUID animalId, Instant fechaSalida, String motivo) {
        MembresiaLote actual = lotes.findActiveMembership(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_IN_LOT));
        if (!actual.loteId().equals(lote.id())) throw new BusinessException(ErrorCode.ANIMAL_NOT_IN_LOT);
        if (fechaSalida.isBefore(actual.fechaIngreso())) throw new BusinessException(ErrorCode.INVALID_MEMBERSHIP_DATE);
        lotes.closeMembership(lote.id(), lote.codigo(), animalId, user.empresaId(), motivo, fechaSalida, user.userId());
        animales.updateLote(animalId, user.empresaId(), null, user.userId());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loteId", lote.id().toString());
        metadata.put("loteCodigo", lote.codigo());
        metadata.put("membresiaId", actual.id().toString());
        metadata.put("fechaSalida", fechaSalida.toString());
        metadata.put("motivo", motivo);
        publicarLote(user, animalId, TipoEventoAnimal.LOTE_REMOVIDO, "Salida del lote",
                actual.id(), metadata);
        audit(user, "RETIRAR_ANIMAL_LOTE", lote.id());
    }

    private void publicarLote(CurrentUser user, UUID animalId, TipoEventoAnimal tipo, String descripcion,
                              UUID registroOrigenId, Map<String, Object> metadata) {
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), animalId, tipo, null, descripcion, null,
                registroOrigenId, metadata, user.userId(), Instant.now(), null));
    }

    private List<UUID> requireUnique(List<UUID> animalIds) {
        if (animalIds == null || animalIds.isEmpty()) throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        List<UUID> unique = new ArrayList<>(new LinkedHashSet<>(animalIds));
        if (unique.size() != animalIds.size()) throw new BusinessException(ErrorCode.DUPLICATE_ANIMAL_IN_REQUEST);
        return unique;
    }

    private ModoIngreso parseModo(String modo) {
        try {
            return ModoIngreso.valueOf(modo.toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> animales.findByIdAnyCompany(animalId).isPresent()
                        ? new BusinessException(ErrorCode.ANIMAL_COMPANY_MISMATCH)
                        : new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
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
