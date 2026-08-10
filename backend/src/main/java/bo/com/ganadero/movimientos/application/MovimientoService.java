package bo.com.ganadero.movimientos.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.movimientos.domain.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MovimientoService {
    private final MovimientoRepository movimientos;
    private final AnimalRepository animales;
    private final LoteRepository lotes;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;

    public MovimientoService(MovimientoRepository movimientos, AnimalRepository animales, LoteRepository lotes,
                             UserContext context, ApplicationEventPublisher events, TimelineEventPublisher timeline) {
        this.movimientos = movimientos;
        this.animales = animales;
        this.lotes = lotes;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
    }

    @Transactional(readOnly = true)
    public MovimientoPage list(EstadoMovimiento estado, TipoMovimiento tipo, int page, int size) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_VER");
        return movimientos.findAll(user.empresaId(), estado, tipo, page, size);
    }

    @Transactional(readOnly = true)
    public Movimiento get(UUID id) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_VER");
        return require(id, user.empresaId());
    }

    @Transactional(readOnly = true)
    public List<MovimientoDetalle> detalles(UUID id) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_VER");
        require(id, user.empresaId());
        return movimientos.findDetalles(id);
    }

    @Transactional
    public Movimiento create(MovimientoCommand command) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_CREAR");
        if (command.animales() == null || command.animales().isEmpty()) {
            throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        }
        Set<UUID> unicos = new HashSet<>();
        for (MovimientoAnimal animal : command.animales()) {
            if (!unicos.add(animal.animalId())) throw new BusinessException(ErrorCode.DUPLICATE_ANIMAL_IN_MOVEMENT);
        }
        Movimiento value = new Movimiento(command.id() != null ? command.id() : UUID.randomUUID(),
                user.empresaId(), command.tipo(), EstadoMovimiento.PENDIENTE,
                command.fechaMovimiento() == null ? LocalDate.now() : command.fechaMovimiento(),
                command.motivo(), command.observacion(),
                command.origenPropiedadId(), command.origenPotreroId(), command.origenLoteId(),
                command.destinoPropiedadId(), command.destinoPotreroId(), command.destinoLoteId(),
                user.userId(), null, null, null, null, null, null, null, null, null, null, 0);
        validateDestino(value);
        for (MovimientoAnimal ma : command.animales()) {
            Animal animal = requireAnimal(user, ma.animalId());
            validateOrigen(user, animal, value);
            if (value.destinoPotreroId() != null && value.destinoPropiedadId() != null
                    && !animales.validLocation(user.empresaId(), value.destinoPropiedadId(), value.destinoPotreroId())) {
                throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
            }
            if (value.destinoLoteId() != null && value.destinoLoteId().equals(animal.loteActualId())) {
                throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
            }
        }
        Movimiento saved = movimientos.create(value, command.animales(), user.userId());
        audit(user, "CREAR", saved.id());
        return saved;
    }

    @Transactional
    public ValidacionMovimiento validar(UUID id) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_CONFIRMAR");
        Movimiento movimiento = require(id, user.empresaId());
        if (movimiento.estado() == EstadoMovimiento.CONFIRMADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_CONFIRMED);
        if (movimiento.estado() == EstadoMovimiento.ANULADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_ANNULLED);
        if (movimiento.estado() == EstadoMovimiento.REVERTIDO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_REVERTED);
        List<MovimientoDetalle> detalles = movimientos.findDetalles(id);
        List<ValidacionAnimalResult> resultados = new ArrayList<>();
        for (MovimientoDetalle detalle : detalles) {
            resultados.add(validarAnimal(user, movimiento, detalle, false));
        }
        audit(user, "VALIDAR", movimiento.id());
        return ValidacionMovimiento.of(resultados);
    }

    @Transactional
    public Movimiento confirm(UUID id, long version) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_CONFIRMAR");
        Movimiento movimiento = requireForUpdate(id, user.empresaId());
        if (movimiento.estado() == EstadoMovimiento.CONFIRMADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_CONFIRMED);
        if (movimiento.estado() == EstadoMovimiento.ANULADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_ANNULLED);
        if (movimiento.estado() == EstadoMovimiento.REVERTIDO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_REVERTED);
        MovimientoStatePolicy.require(movimiento.estado(), EstadoMovimiento.CONFIRMADO);
        if (movimiento.destinoPropiedadId() != null) context.requirePropertyAccess(user, movimiento.destinoPropiedadId());

        List<MovimientoDetalle> detalles = movimientos.findDetalles(id);
        if (detalles.isEmpty()) throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        List<Animal> animalesConfirmados = new ArrayList<>();
        for (MovimientoDetalle detalle : detalles) {
            ValidacionAnimalResult resultado = validarAnimal(user, movimiento, detalle, true);
            if (!resultado.valido()) throw new BusinessException(resultado.error(), resultado.mensaje());
            animalesConfirmados.add(resultado.animal());
        }
        List<MovimientoDetalle> snapshots = new ArrayList<>();
        for (int i = 0; i < detalles.size(); i++) {
            MovimientoDetalle detalle = detalles.get(i);
            Animal animal = animalesConfirmados.get(i);
            Ubicacion destino = destinoEfectivo(user, animal, movimiento);
            snapshots.add(new MovimientoDetalle(detalle.id(), detalle.movimientoId(), detalle.animalId(),
                    animal.version(), detalle.estadoAntes(), detalle.estadoDespues(),
                    animal.propiedadActualId(), animal.potreroActualId(), animal.loteActualId(),
                    destino.propiedad(), destino.potrero(), destino.lote(), "OK", null));
        }
        for (Animal animal : animalesConfirmados) {
            applyMovimiento(user, animal, movimiento);
        }
        movimientos.saveDetalleUbicaciones(id, snapshots);
        Movimiento saved = movimientos.confirm(id, user.empresaId(), version, user.userId());
        audit(user, "CONFIRMAR", saved.id());
        return saved;
    }

    @Transactional
    public Movimiento annul(UUID id, String motivo, long version) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_ANULAR");
        Movimiento movimiento = requireForUpdate(id, user.empresaId());
        if (movimiento.estado() == EstadoMovimiento.CONFIRMADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_CONFIRMED);
        if (movimiento.estado() == EstadoMovimiento.ANULADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_ANNULLED);
        if (movimiento.estado() == EstadoMovimiento.REVERTIDO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_REVERTED);
        MovimientoStatePolicy.require(movimiento.estado(), EstadoMovimiento.ANULADO);
        Movimiento saved = movimientos.annul(id, user.empresaId(), motivo, version, user.userId());
        audit(user, "ANULAR", saved.id());
        return saved;
    }

    @Transactional
    public Movimiento revert(UUID id, String motivo, long version) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_REVERTIR");
        Movimiento original = requireForUpdate(id, user.empresaId());
        if (original.estado() == EstadoMovimiento.REVERTIDO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_REVERTED);
        if (original.estado() != EstadoMovimiento.CONFIRMADO) throw new BusinessException(ErrorCode.MOVEMENT_REVERT_NOT_ALLOWED);
        if (!original.tipo().esReversible()) throw new BusinessException(ErrorCode.MOVEMENT_REVERT_NOT_ALLOWED);
        if (original.movimientoReversionId() != null || movimientos.findByOriginal(id, user.empresaId()).isPresent()) {
            throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_REVERTED);
        }
        if (original.destinoPropiedadId() != null) context.requirePropertyAccess(user, original.destinoPropiedadId());
        if (original.origenPropiedadId() != null) context.requirePropertyAccess(user, original.origenPropiedadId());

        List<MovimientoDetalle> detalles = movimientos.findDetalles(id);
        for (MovimientoDetalle detalle : detalles) {
            revertAnimal(user, detalle, original);
        }

        UUID reversionId = UUID.randomUUID();
        Instant ahora = Instant.now();
        Movimiento reversion = new Movimiento(reversionId, user.empresaId(), original.tipo().inverso(),
                EstadoMovimiento.CONFIRMADO, LocalDate.now(),
                "Reversión de " + original.id() + (motivo == null ? "" : ": " + motivo), null,
                original.destinoPropiedadId(), original.destinoPotreroId(), original.destinoLoteId(),
                original.origenPropiedadId(), original.origenPotreroId(), original.origenLoteId(),
                user.userId(), user.userId(), null, ahora, null, null,
                user.userId(), ahora, motivo, original.id(), null, 0);
        List<MovimientoAnimal> inversos = detalles.stream()
                .map(detalle -> new MovimientoAnimal(detalle.animalId(), detalle.animalVersionEsperada())).toList();
        movimientos.saveConfirmed(reversion, inversos, user.userId());
        Movimiento saved = movimientos.markReverted(original.id(), user.empresaId(), reversionId, motivo, original.version(), user.userId());
        for (MovimientoDetalle detalle : detalles) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("motivo", motivo == null ? "" : motivo);
            metadata.put("tipoMovimiento", original.tipo().name());
            metadata.put("movimientoInverso", reversion.id().toString());
            timeline.publish(new RegistrarEventoTimeline(user.empresaId(), detalle.animalId(),
                    TipoEventoAnimal.MOVIMIENTO_REVERTIDO, null,
                    "Revertido con motivo: " + (motivo == null ? "" : motivo), null, reversion.id(), metadata,
                    user.userId(), Instant.now(), null));
        }
        audit(user, "REVERTIR", original.id());
        return saved;
    }

    private ValidacionAnimalResult validarAnimal(CurrentUser user, Movimiento movimiento, MovimientoDetalle detalle,
                                                 boolean lock) {
        try {
            Animal animal;
            if (lock) {
                animal = animales.findByIdForUpdate(detalle.animalId(), user.empresaId()).orElse(null);
            } else {
                animal = animales.findById(detalle.animalId(), user.empresaId()).orElse(null);
            }
            if (animal == null) throw new BusinessException(ErrorCode.ANIMAL_NOT_FOUND);
            context.requirePropertyAccess(user, animal.propiedadActualId());
            if (animal.estado() == EstadoAnimal.MUERTO || animal.estado() == EstadoAnimal.VENDIDO
                    || animal.estado() == EstadoAnimal.DESCARTADO) {
                throw new BusinessException(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED,
                        "El animal está " + animal.estado().name() + " y no puede moverse.");
            }
            if (detalle.animalVersionEsperada() != 0 && animal.version() != detalle.animalVersionEsperada()) {
                throw new BusinessException(ErrorCode.ANIMAL_VERSION_CONFLICT);
            }
            validateOrigen(user, animal, movimiento);
            if (movimiento.tipo() == TipoMovimiento.TRANSFERENCIA_PROPIEDAD
                    && movimiento.destinoPropiedadId() != null
                    && movimiento.destinoPropiedadId().equals(animal.propiedadActualId())) {
                throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION,
                        "La propiedad de destino debe ser distinta de la actual.");
            }
            if (movimiento.tipo() == TipoMovimiento.CAMBIO_POTRERO && movimiento.origenPotreroId() != null
                    && movimiento.destinoPotreroId() != null
                    && movimiento.destinoPotreroId().equals(movimiento.origenPotreroId())) {
                throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION,
                        "El potrero de destino debe ser distinto del origen.");
            }
            return ValidacionAnimalResult.valid(animal);
        } catch (BusinessException ex) {
            return ValidacionAnimalResult.invalid(detalle.animalId(), ex.code(), ex.getMessage());
        }
    }

    private void revertAnimal(CurrentUser user, MovimientoDetalle detalle, Movimiento original) {
        Animal animal = requireAnimalForUpdate(user, detalle.animalId());
        if (detalle.propiedadDespues() != null && !detalle.propiedadDespues().equals(animal.propiedadActualId())) {
            throw new BusinessException(ErrorCode.MOVEMENT_CANNOT_BE_REVERSED);
        }
        if (detalle.potreroDespues() != null && !detalle.potreroDespues().equals(animal.potreroActualId())) {
            throw new BusinessException(ErrorCode.MOVEMENT_CANNOT_BE_REVERSED);
        }
        if (detalle.loteDespues() != null) {
            boolean sigueEnLote = lotes.findActiveLotOfAnimal(animal.id(), user.empresaId())
                    .map(lote -> lote.id().equals(detalle.loteDespues())).orElse(false);
            if (!sigueEnLote) throw new BusinessException(ErrorCode.MOVEMENT_CANNOT_BE_REVERSED);
        }
        if (detalle.loteDespues() != null && !detalle.loteDespues().equals(detalle.loteAntes())) {
            lotes.closeMembership(detalle.loteDespues(), null, animal.id(), user.empresaId(),
                    "Reversión de movimiento " + original.tipo().name(), Instant.now(), user.userId());
        }
        if (detalle.loteAntes() != null && !detalle.loteAntes().equals(detalle.loteDespues())) {
            lotes.openMembership(detalle.loteAntes(), null, animal.id(), user.empresaId(),
                    "Reversión de movimiento", null, "PARCIAL", Instant.now(), user.userId());
        }
        if (original.tipo() == TipoMovimiento.SALIDA_VENTA && animal.estado() == EstadoAnimal.VENDIDO) {
            animales.changeState(animal.id(), user.empresaId(), animal.estado(), EstadoAnimal.ACTIVO,
                    "Reversión de movimiento", animal.version(), user.userId());
        }
        animales.restoreLocation(animal.id(), user.empresaId(), detalle.propiedadAntes(),
                detalle.potreroAntes(), detalle.loteAntes(), user.userId());
    }

    private void validateDestino(Movimiento movimiento) {
        boolean hasDestino = movimiento.destinoPropiedadId() != null || movimiento.destinoPotreroId() != null
                || movimiento.destinoLoteId() != null;
        if (!hasDestino) throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
        switch (movimiento.tipo()) {
            case CAMBIO_POTRERO, CUARENTENA, RETORNO_CUARENTENA -> {
                if (movimiento.destinoPotreroId() == null) throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
            }
            case CAMBIO_LOTE -> {
                if (movimiento.destinoLoteId() == null) throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
            }
            case INGRESO_COMPRA, TRANSFERENCIA_PROPIEDAD -> {
                if (movimiento.destinoPropiedadId() == null) throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
            }
            case SALIDA_VENTA -> { }
        }
        if (movimiento.destinoPotreroId() != null && movimiento.destinoPropiedadId() == null
                && movimiento.tipo() != TipoMovimiento.CAMBIO_POTRERO
                && movimiento.tipo() != TipoMovimiento.CAMBIO_LOTE
                && movimiento.tipo() != TipoMovimiento.CUARENTENA
                && movimiento.tipo() != TipoMovimiento.RETORNO_CUARENTENA) {
            throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION,
                    "El potrero de destino requiere la propiedad de destino.");
        }
    }

    private void validateOrigen(CurrentUser user, Animal animal, Movimiento movimiento) {
        if (movimiento.origenPropiedadId() != null && !movimiento.origenPropiedadId().equals(animal.propiedadActualId())) {
            throw new BusinessException(ErrorCode.INVALID_MOVEMENT_ORIGIN);
        }
        if (movimiento.origenPotreroId() != null && !movimiento.origenPotreroId().equals(animal.potreroActualId())) {
            throw new BusinessException(ErrorCode.INVALID_MOVEMENT_ORIGIN);
        }
        if (movimiento.origenLoteId() != null) {
            boolean coincide = lotes.findActiveLotOfAnimal(animal.id(), user.empresaId())
                    .map(lote -> lote.id().equals(movimiento.origenLoteId())).orElse(false);
            if (!coincide) throw new BusinessException(ErrorCode.INVALID_MOVEMENT_ORIGIN);
        }
    }

    private Ubicacion destinoEfectivo(CurrentUser user, Animal animal, Movimiento movimiento) {
        UUID property = movimiento.destinoPropiedadId() != null ? movimiento.destinoPropiedadId() : animal.propiedadActualId();
        UUID paddock = movimiento.destinoPotreroId() != null ? movimiento.destinoPotreroId() : animal.potreroActualId();
        UUID lote = animal.loteActualId();
        if (movimiento.destinoPotreroId() != null
                && !animales.validLocation(user.empresaId(), property, paddock)) {
            throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION,
                    "El potrero de destino no pertenece a la propiedad de destino.");
        }
        if (movimiento.destinoLoteId() != null) {
            Lote destino = lotes.findById(movimiento.destinoLoteId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
            if (destino.estado() != EstadoLote.ACTIVO) throw new BusinessException(ErrorCode.LOT_ALREADY_CLOSED);
            if (destino.propiedadId() != null && animal.propiedadActualId() != null
                    && !destino.propiedadId().equals(animal.propiedadActualId())) {
                throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION,
                        "El lote de destino no pertenece a la propiedad actual del animal.");
            }
            lote = destino.id();
        }
        return new Ubicacion(property, paddock, lote);
    }

    private void applyMovimiento(CurrentUser user, Animal animal, Movimiento movimiento) {
        Ubicacion destino = destinoEfectivo(user, animal, movimiento);
        if (movimiento.destinoLoteId() != null) {
            lotes.findActiveLotOfAnimal(animal.id(), user.empresaId()).ifPresent(oldLote -> {
                if (!oldLote.id().equals(destino.lote())) {
                    lotes.closeMembership(oldLote.id(), null, animal.id(), user.empresaId(),
                            "Movimiento " + movimiento.tipo().name(), Instant.now(), user.userId());
                }
            });
            if (destino.lote() != null) {
                lotes.openMembership(destino.lote(), null, animal.id(), user.empresaId(),
                        "Movimiento " + movimiento.tipo().name(), null, "PARCIAL",
                        Instant.now(), user.userId());
            }
        }
        animales.move(animal.id(), user.empresaId(), destino.propiedad(), destino.potrero(), destino.lote(), user.userId());
        if (movimiento.tipo() == TipoMovimiento.SALIDA_VENTA && animal.estado() == EstadoAnimal.ACTIVO) {
            animales.changeState(animal.id(), user.empresaId(), animal.estado(), EstadoAnimal.VENDIDO,
                    "Venta: " + (movimiento.motivo() == null ? "" : movimiento.motivo()),
                    animal.version(), user.userId());
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", movimiento.tipo().name());
        metadata.put("fechaMovimiento", movimiento.fechaMovimiento().toString());
        metadata.put("destinoPropiedadId", destino.propiedad() == null ? "" : destino.propiedad().toString());
        metadata.put("destinoPotreroId", destino.potrero() == null ? "" : destino.potrero().toString());
        metadata.put("destinoLoteId", destino.lote() == null ? "" : destino.lote().toString());
        if (movimiento.motivo() != null) metadata.put("motivo", movimiento.motivo());
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), animal.id(),
                movimiento.tipo() == TipoMovimiento.CUARENTENA ? TipoEventoAnimal.CUARENTENA_INICIADA
                        : movimiento.tipo() == TipoMovimiento.RETORNO_CUARENTENA ? TipoEventoAnimal.CUARENTENA_FINALIZADA
                        : TipoEventoAnimal.MOVIMIENTO_REGISTRADO,
                null, movimiento.motivo(), null, movimiento.id(), metadata, user.userId(), Instant.now(), null));
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private Animal requireAnimalForUpdate(CurrentUser user, UUID animalId) {
        Animal animal = animales.findByIdForUpdate(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private Movimiento require(UUID id, UUID empresa) {
        return movimientos.findById(id, empresa).orElseThrow(() -> new BusinessException(ErrorCode.MOVEMENT_NOT_FOUND));
    }

    private Movimiento requireForUpdate(UUID id, UUID empresa) {
        return movimientos.findByIdForUpdate(id, empresa).orElseThrow(() -> new BusinessException(ErrorCode.MOVEMENT_NOT_FOUND));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new MovimientoAuditEvent(user.empresaId(), user.userId(), accion, "MOVIMIENTO", id, Instant.now()));
    }

    private record Ubicacion(UUID propiedad, UUID potrero, UUID lote) {}
}
