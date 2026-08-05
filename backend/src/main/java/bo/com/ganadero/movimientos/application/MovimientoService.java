package bo.com.ganadero.movimientos.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.movimientos.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MovimientoService {
    private final MovimientoRepository movimientos;
    private final AnimalRepository animales;
    private final LoteRepository lotes;
    private final UserContext context;
    private final ApplicationEventPublisher events;

    public MovimientoService(MovimientoRepository movimientos, AnimalRepository animales, LoteRepository lotes,
                             UserContext context, ApplicationEventPublisher events) {
        this.movimientos = movimientos;
        this.animales = animales;
        this.lotes = lotes;
        this.context = context;
        this.events = events;
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
        if (command.animalIds() == null || command.animalIds().isEmpty()) {
            throw new BusinessException(ErrorCode.MOVEMENT_EMPTY);
        }
        validateDestino(command);
        Set<UUID> uniqueIds = new LinkedHashSet<>(command.animalIds());
        for (UUID animalId : uniqueIds) {
            Animal animal = requireAnimal(user, animalId);
            if (command.origenPropiedadId() != null) context.requirePropertyAccess(user, command.origenPropiedadId());
            if (command.origenPotreroId() != null && animal.potreroActualId() != null
                    && !command.origenPotreroId().equals(animal.potreroActualId())) {
                throw new BusinessException(ErrorCode.INVALID_MOVEMENT_ORIGIN);
            }
        }
        UUID id = command.id() != null ? command.id() : UUID.randomUUID();
        Movimiento value = new Movimiento(id, user.empresaId(), command.tipo(), EstadoMovimiento.PENDIENTE,
                command.fechaMovimiento() == null ? LocalDate.now() : command.fechaMovimiento(), command.motivo(),
                command.origenPropiedadId(), command.origenPotreroId(), command.origenLoteId(),
                command.destinoPropiedadId(), command.destinoPotreroId(), command.destinoLoteId(),
                user.userId(), null, null, null, null, null, 0);
        Movimiento saved = movimientos.create(value, List.copyOf(uniqueIds), user.userId());
        audit(user, "CREAR", saved.id());
        return saved;
    }

    @Transactional
    public Movimiento confirm(UUID id, long version) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_CONFIRMAR");
        Movimiento movimiento = require(id, user.empresaId());
        if (movimiento.estado() == EstadoMovimiento.CONFIRMADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_CONFIRMED);
        if (movimiento.estado() == EstadoMovimiento.ANULADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_ANNULLED);
        if (movimiento.destinoPropiedadId() != null) context.requirePropertyAccess(user, movimiento.destinoPropiedadId());
        if (movimiento.origenPropiedadId() != null) context.requirePropertyAccess(user, movimiento.origenPropiedadId());

        List<MovimientoDetalle> detalles = movimientos.findDetalles(id);
        for (MovimientoDetalle detalle : detalles) {
            Animal animal = requireAnimal(user, detalle.animalId());
            validateOrigen(user, animal, movimiento);
        }
        for (MovimientoDetalle detalle : detalles) {
            applyMovimiento(user, detalle.animalId(), movimiento);
        }
        Movimiento saved = movimientos.confirm(id, user.empresaId(), version, user.userId());
        audit(user, "CONFIRMAR", saved.id());
        return saved;
    }

    @Transactional
    public Movimiento annul(UUID id, String motivo, long version) {
        CurrentUser user = context.requirePermission("MOVIMIENTO_ANULAR");
        Movimiento movimiento = require(id, user.empresaId());
        if (movimiento.estado() == EstadoMovimiento.CONFIRMADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_CONFIRMED);
        if (movimiento.estado() == EstadoMovimiento.ANULADO) throw new BusinessException(ErrorCode.MOVEMENT_ALREADY_ANNULLED);
        Movimiento saved = movimientos.annul(id, user.empresaId(), motivo, version, user.userId());
        audit(user, "ANULAR", saved.id());
        return saved;
    }

    private void validateDestino(MovimientoCommand command) {
        boolean hasDestino = command.destinoPropiedadId() != null || command.destinoPotreroId() != null
                || command.destinoLoteId() != null;
        if (!hasDestino) throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
        if (command.destinoPotreroId() != null && command.destinoPropiedadId() == null
                && command.tipo() != TipoMovimiento.CAMBIO_POTRERO && command.tipo() != TipoMovimiento.CAMBIO_LOTE) {
            throw new BusinessException(ErrorCode.INVALID_MOVEMENT_DESTINATION);
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
            lotes.findActiveLotOfAnimal(animal.id(), user.empresaId())
                    .filter(lote -> lote.id().equals(movimiento.origenLoteId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_MOVEMENT_ORIGIN));
        }
    }

    private void applyMovimiento(CurrentUser user, UUID animalId, Movimiento movimiento) {
        Animal animal = requireAnimal(user, animalId);
        UUID property = movimiento.destinoPropiedadId() != null ? movimiento.destinoPropiedadId() : animal.propiedadActualId();
        UUID paddock = movimiento.destinoPotreroId() != null ? movimiento.destinoPotreroId() : animal.potreroActualId();
        UUID loteDestino;
        if (movimiento.destinoLoteId() != null) {
            Lote destino = lotes.findById(movimiento.destinoLoteId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
            if (destino.estado() != EstadoLote.ABIERTO) throw new BusinessException(ErrorCode.LOT_ALREADY_CLOSED);
            loteDestino = destino.id();
        } else {
            loteDestino = null;
        }
        if (loteDestino != null) {
            lotes.findActiveLotOfAnimal(animalId, user.empresaId()).ifPresent(oldLote -> {
                if (!oldLote.id().equals(loteDestino)) {
                    lotes.closeMembership(oldLote.id(), animalId, user.empresaId(), "Movimiento " + movimiento.tipo().name(), user.userId());
                }
            });
        }
        animales.move(animalId, user.empresaId(), property, paddock, loteDestino, user.userId());
        if (loteDestino != null) {
            lotes.openMembership(loteDestino, animalId, user.empresaId(), user.userId());
        }
        movimientos.insertEvent(animalId, movimiento, user.userId());
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private Movimiento require(UUID id, UUID empresa) {
        return movimientos.findById(id, empresa).orElseThrow(() -> new BusinessException(ErrorCode.MOVEMENT_NOT_FOUND));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new MovimientoAuditEvent(user.empresaId(), user.userId(), accion, "MOVIMIENTO", id, Instant.now()));
    }
}
