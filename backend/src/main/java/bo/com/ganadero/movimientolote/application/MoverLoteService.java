package bo.com.ganadero.movimientolote.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.lotes.domain.MembresiaLote;
import bo.com.ganadero.movimientolote.domain.*;
import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * "Mover lote": traslado colectivo (total o parcial) de los miembros de un lote ganadero.
 *
 * <p>Orquesta primitivas ya existentes de otros módulos (todas públicas): {@link AnimalRepository}
 * (elegibilidad, {@code move}), {@link LoteRepository} (membresías, proyección de ubicación
 * operativa) y {@link MovimientoRepository} (encabezado + detalle del movimiento, ya soporta
 * un movimiento con muchos animales). No reutiliza {@code MovimientoService.create()+confirm()}
 * porque su inferencia de "lote destino" (docs: si no cambia de propiedad conserva el lote
 * actual; si cambia, lo limpia) no distingue las 4 acciones explícitas que pide esta operación
 * (mantener / cambiar a lote existente / crear uno nuevo / dejar sin lote) — construir el
 * {@link Movimiento} directamente aquí evita forzar esa inferencia a un caso que no cubre.</p>
 */
@Service
public class MoverLoteService {
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");

    private final PreparacionMovimientoLoteRepository preparaciones;
    private final LoteRepository lotes;
    private final AnimalRepository animales;
    private final MovimientoRepository movimientos;
    private final MovimientoService movimientoService;
    private final UserContext context;
    private final CodigoService codigos;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;
    private final ObjectProvider<RestriccionSanitariaPort> restriccionSanitaria;

    @Value("${ganadero.lotes.preparacion-expiracion-minutos:30}")
    private long preparacionExpiracionMinutos = 30;

    public MoverLoteService(PreparacionMovimientoLoteRepository preparaciones, LoteRepository lotes,
                            AnimalRepository animales, MovimientoRepository movimientos,
                            MovimientoService movimientoService, UserContext context,
                            CodigoService codigos, ApplicationEventPublisher events, TimelineEventPublisher timeline,
                            ObjectProvider<RestriccionSanitariaPort> restriccionSanitaria) {
        this.preparaciones = preparaciones;
        this.lotes = lotes;
        this.animales = animales;
        this.movimientos = movimientos;
        this.movimientoService = movimientoService;
        this.context = context;
        this.codigos = codigos;
        this.events = events;
        this.timeline = timeline;
        this.restriccionSanitaria = restriccionSanitaria;
    }

    @Transactional
    public PreparacionResultado preparar(UUID loteId, PrepararMovimientoLoteCommand cmd) {
        CurrentUser user = context.requirePermission("LOTE_MOVER");
        Lote lote = requireLote(loteId, user.empresaId());
        context.requirePropertyAccess(user, lote.propiedadId());
        context.requirePropertyAccess(user, cmd.destinoPropiedadId());
        if (lote.estado() != EstadoLote.ACTIVO) throw new BusinessException(ErrorCode.LOT_CLOSED);
        validarDestinoBasico(user, cmd);

        List<MembresiaLote> membresias = lotes.findMemberships(loteId, user.empresaId(), true);
        List<PreparacionMovimientoLoteMiembro> miembros = new ArrayList<>();
        for (MembresiaLote m : membresias) {
            Animal animal = animales.findById(m.animalId(), user.empresaId()).orElse(null);
            UUID miembroId = UUID.randomUUID();
            if (animal == null) {
                miembros.add(new PreparacionMovimientoLoteMiembro(miembroId, null, m.animalId(), null, null, null,
                        null, null, loteId, 0, false,
                        "El animal no pertenece a esta empresa o ya no existe.", false, List.of(), Instant.now()));
                continue;
            }
            Elegibilidad e = evaluarElegibilidad(animal, lote, user);
            miembros.add(new PreparacionMovimientoLoteMiembro(miembroId, null, animal.id(), animal.codigo(),
                    animal.nombre(), animal.estado().name(), animal.propiedadActualId(), animal.potreroActualId(),
                    loteId, animal.version(), e.elegible(), e.motivoExclusion(), e.elegible(), e.restricciones(),
                    Instant.now()));
        }

        UUID prepId = UUID.randomUUID();
        Instant ahora = Instant.now();
        ModalidadMovimientoLote modalidad = cmd.modalidadDeclarada() == null
                ? ModalidadMovimientoLote.LOTE_COMPLETO : cmd.modalidadDeclarada();
        PreparacionMovimientoLote prep = new PreparacionMovimientoLote(prepId, loteId, lote.propiedadId(),
                lote.potreroActualId(), modalidad, cmd.destinoPropiedadId(), cmd.destinoPotreroId(), cmd.accionLote(),
                cmd.loteDestinoId(), cmd.nuevoLote() == null ? null : cmd.nuevoLote().nombre(),
                cmd.nuevoLote() == null ? null : cmd.nuevoLote().codigo(),
                cmd.nuevoLote() == null ? null : cmd.nuevoLote().descripcion(),
                cmd.fechaEfectiva() == null ? ahora : cmd.fechaEfectiva(), cmd.motivo(), cmd.observaciones(),
                EstadoPreparacionLote.VIGENTE, ahora, ahora.plus(Duration.ofMinutes(preparacionExpiracionMinutos)),
                null, null, user.userId(), 0);
        preparaciones.crear(prep, miembros, user.userId());
        audit(user, "PREPARAR_MOVIMIENTO_LOTE", loteId);
        return armarResultado(prep, miembros);
    }

    @Transactional(readOnly = true)
    public PreparacionResultado obtenerPreparacion(UUID preparacionId) {
        CurrentUser user = context.requirePermission("LOTE_MOVER");
        PreparacionMovimientoLote prep = requirePreparacion(preparacionId);
        requireLote(prep.loteOrigenId(), user.empresaId());
        return armarResultado(prep, preparaciones.findMiembros(preparacionId));
    }

    @Transactional
    public ResultadoMovimientoLote confirmar(UUID preparacionId, ConfirmarMovimientoLoteCommand cmd) {
        CurrentUser user = context.requirePermission("LOTE_MOVER");
        PreparacionMovimientoLote prep = preparaciones.findByIdForUpdate(preparacionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREPARACION_LOTE_NOT_FOUND));
        if (prep.estado() != EstadoPreparacionLote.VIGENTE) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_ESTADO_INVALIDO,
                    "La preparación ya está en estado " + prep.estado() + "; prepara el movimiento nuevamente.");
        }
        if (Instant.now().isAfter(prep.fechaExpiracion())) {
            preparaciones.marcarEstado(preparacionId, EstadoPreparacionLote.EXPIRADA, prep.version(), user.userId());
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_EXPIRADA);
        }
        if (cmd.version() != prep.version()) throw new BusinessException(ErrorCode.VERSION_CONFLICT);

        Lote loteOrigen = requireLote(prep.loteOrigenId(), user.empresaId());
        context.requirePropertyAccess(user, loteOrigen.propiedadId());
        context.requirePropertyAccess(user, prep.destinoPropiedadId());
        if (prep.fechaEfectiva().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_MEMBERSHIP_DATE, "La fecha efectiva no puede ser futura.");
        }

        List<PreparacionMovimientoLoteMiembro> fotografia = preparaciones.findMiembros(preparacionId);
        Map<UUID, PreparacionMovimientoLoteMiembro> porAnimal = fotografia.stream()
                .filter(m -> m.animalId() != null)
                .collect(Collectors.toMap(PreparacionMovimientoLoteMiembro::animalId, m -> m));

        List<UUID> seleccionIds = cmd.animalIds() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(cmd.animalIds()));
        if (seleccionIds.isEmpty()) throw new BusinessException(ErrorCode.PREPARACION_LOTE_SIN_SELECCION);
        for (UUID id : seleccionIds) {
            PreparacionMovimientoLoteMiembro m = porAnimal.get(id);
            if (m == null || !m.elegible()) {
                throw new BusinessException(ErrorCode.PREPARACION_LOTE_SELECCION_INVALIDA,
                        "Un animal seleccionado no forma parte de los elegibles fotografiados en esta preparación.");
            }
        }

        List<Animal> animalesAMover = new ArrayList<>();
        List<String> conflictos = new ArrayList<>();
        for (UUID id : seleccionIds) {
            PreparacionMovimientoLoteMiembro snapshot = porAnimal.get(id);
            Animal animal = animales.findByIdForUpdate(id, user.empresaId()).orElse(null);
            if (animal == null) {
                conflictos.add((snapshot.animalCodigo() == null ? id.toString() : snapshot.animalCodigo())
                        + ": ya no existe o pertenece a otra empresa.");
                continue;
            }
            if (animal.version() != snapshot.animalVersion()) {
                conflictos.add(animal.codigo() + ": fue modificado por otro usuario desde que se preparó el movimiento.");
                continue;
            }
            if (animal.estado() != EstadoAnimal.ACTIVO) {
                conflictos.add(animal.codigo() + ": ya no está activo (" + animal.estado() + ").");
                continue;
            }
            boolean sigueEnLote = lotes.findActiveMembership(id, user.empresaId())
                    .map(mem -> mem.loteId().equals(loteOrigen.id())).orElse(false);
            if (!sigueEnLote) {
                conflictos.add(animal.codigo() + ": ya no pertenece al lote de origen.");
                continue;
            }
            if (animal.fechaIngreso() != null
                    && prep.fechaEfectiva().isBefore(animal.fechaIngreso().atStartOfDay(BOLIVIA).toInstant())) {
                conflictos.add(animal.codigo() + ": la fecha efectiva es anterior a su ingreso al sistema.");
                continue;
            }
            Elegibilidad fresh = evaluarElegibilidad(animal, loteOrigen, user);
            if (!fresh.elegible()) {
                conflictos.add(animal.codigo() + ": " + fresh.motivoExclusion());
                continue;
            }
            for (RestriccionSanitaria r : fresh.restricciones()) {
                if (r.severidad() == SeveridadRestriccion.ADVERTENCIA) {
                    boolean autorizada = cmd.autorizaciones() != null && cmd.autorizaciones().stream()
                            .anyMatch(a -> a.animalId().equals(id) && r.tipo().equals(a.tipoRestriccion())
                                    && a.motivo() != null && !a.motivo().isBlank());
                    if (!autorizada) {
                        conflictos.add(animal.codigo() + ": advertencia sanitaria sin autorizar (" + r.tipo() + ").");
                    }
                }
            }
            animalesAMover.add(animal);
        }
        if (!conflictos.isEmpty()) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_ANIMAL_CAMBIO, String.join(" | ", conflictos));
        }

        long totalElegiblesFotografia = fotografia.stream().filter(PreparacionMovimientoLoteMiembro::elegible).count();
        boolean hayExcluidos = fotografia.stream().anyMatch(m -> !m.elegible());
        boolean esTransferenciaCompleta = !hayExcluidos && seleccionIds.size() == totalElegiblesFotografia;

        if (prep.accionLote() == AccionLote.MANTENER_LOTE && !esTransferenciaCompleta) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_ACCION_INVALIDA,
                    "No puede mantenerse la identidad del lote en un movimiento parcial; elige cambiar a un lote "
                            + "existente, crear uno nuevo o dejar sin lote para los animales seleccionados.");
        }

        UUID loteDestinoResultante;
        Lote loteNuevoCreado = null;
        switch (prep.accionLote()) {
            case MANTENER_LOTE -> loteDestinoResultante = loteOrigen.id();
            case DEJAR_SIN_LOTE -> loteDestinoResultante = null;
            case CAMBIAR_A_LOTE_EXISTENTE -> {
                Lote destino = lotes.findById(prep.loteDestinoId(), user.empresaId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
                if (destino.estado() != EstadoLote.ACTIVO) throw new BusinessException(ErrorCode.LOT_CLOSED);
                if (!destino.propiedadId().equals(prep.destinoPropiedadId())) {
                    throw new BusinessException(ErrorCode.PREPARACION_LOTE_DESTINO_INVALIDO,
                            "El lote de destino no pertenece a la propiedad de destino.");
                }
                loteDestinoResultante = destino.id();
            }
            case CREAR_NUEVO_LOTE -> {
                if (prep.nuevoLoteNombre() == null || prep.nuevoLoteNombre().isBlank()) {
                    throw new BusinessException(ErrorCode.PREPARACION_LOTE_NUEVO_LOTE_DATOS_REQUERIDOS);
                }
                String codigo = codigos.paraCreacion(user, TipoCodigo.LOTE, null, LocalDate.now().getYear(),
                        prep.nuevoLoteCodigo());
                Lote nuevo = new Lote(UUID.randomUUID(), user.empresaId(), prep.destinoPropiedadId(), codigo,
                        prep.nuevoLoteNombre(), prep.nuevoLoteDescripcion(), EstadoLote.ACTIVO, LocalDate.now(), null, 0);
                loteNuevoCreado = lotes.create(nuevo, user.userId());
                loteDestinoResultante = loteNuevoCreado.id();
            }
            default -> throw new BusinessException(ErrorCode.PREPARACION_LOTE_ACCION_INVALIDA);
        }

        if (!animales.validLocation(user.empresaId(), prep.destinoPropiedadId(), prep.destinoPotreroId())) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_DESTINO_INVALIDO,
                    "El potrero de destino ya no pertenece a la propiedad de destino.");
        }

        TipoMovimiento tipo = determinarTipo(loteOrigen, prep, loteDestinoResultante);

        LocalDate fechaMovimiento = prep.fechaEfectiva().atZone(BOLIVIA).toLocalDate();
        Movimiento cabecera = new Movimiento(UUID.randomUUID(), user.empresaId(), tipo, EstadoMovimiento.CONFIRMADO,
                fechaMovimiento, prep.motivo(), prep.observaciones(),
                loteOrigen.propiedadId(), prep.potreroOrigenId(), loteOrigen.id(),
                prep.destinoPropiedadId(), prep.destinoPotreroId(), loteDestinoResultante,
                user.userId(), user.userId(), null, Instant.now(), null, null, null, null, null, null, null, 0);
        List<MovimientoAnimal> animalesMov = animalesAMover.stream()
                .map(a -> new MovimientoAnimal(a.id(), a.version())).toList();
        Movimiento guardado = movimientos.saveConfirmed(cabecera, animalesMov, user.userId());
        Map<UUID, MovimientoDetalle> detallePorAnimal = movimientos.findDetalles(guardado.id()).stream()
                .collect(Collectors.toMap(MovimientoDetalle::animalId, d -> d));

        List<MovimientoDetalle> snapshots = new ArrayList<>();
        for (Animal animal : animalesAMover) {
            MovimientoDetalle base = detallePorAnimal.get(animal.id());
            UUID loteAntes = animal.loteActualId();
            if (!Objects.equals(loteAntes, loteDestinoResultante)) {
                if (loteAntes != null) {
                    lotes.closeMembership(loteAntes, null, animal.id(), user.empresaId(),
                            motivoMovimiento(prep, tipo), prep.fechaEfectiva(), user.userId());
                }
                if (loteDestinoResultante != null) {
                    lotes.openMembership(loteDestinoResultante, null, animal.id(), user.empresaId(),
                            motivoMovimiento(prep, tipo), prep.observaciones(), "PARCIAL", prep.fechaEfectiva(), user.userId());
                }
            }
            animales.move(animal.id(), user.empresaId(), prep.destinoPropiedadId(), prep.destinoPotreroId(),
                    loteDestinoResultante, user.userId());
            snapshots.add(new MovimientoDetalle(base.id(), base.movimientoId(), animal.id(), animal.version(),
                    EstadoAnimal.ACTIVO, EstadoAnimal.ACTIVO,
                    animal.propiedadActualId(), animal.potreroActualId(), loteAntes,
                    prep.destinoPropiedadId(), prep.destinoPotreroId(), loteDestinoResultante, "OK", null));
            publicarEventoAnimal(user, animal, loteAntes, loteDestinoResultante, tipo, guardado.id(), preparacionId, prep);
        }
        movimientos.saveDetalleUbicaciones(guardado.id(), snapshots);

        lotes.recomputarUbicacionOperativa(loteOrigen.id(), user.userId());
        if (loteDestinoResultante != null && !loteDestinoResultante.equals(loteOrigen.id())) {
            lotes.recomputarUbicacionOperativa(loteDestinoResultante, user.userId());
        }
        if (prep.accionLote() == AccionLote.MANTENER_LOTE && esTransferenciaCompleta
                && !loteOrigen.propiedadId().equals(prep.destinoPropiedadId())) {
            lotes.transferirPropiedad(loteOrigen.id(), prep.destinoPropiedadId(), user.userId());
        }

        if (cmd.autorizaciones() != null) {
            for (AutorizacionCommand a : cmd.autorizaciones()) {
                preparaciones.registrarAutorizacion(preparacionId, a.animalId(), a.tipoRestriccion(), a.motivo(),
                        user.userId());
            }
        }

        preparaciones.confirmar(preparacionId, guardado.id(), loteNuevoCreado == null ? null : loteNuevoCreado.id(),
                prep.version(), user.userId());
        audit(user, "CONFIRMAR_MOVIMIENTO_LOTE", loteOrigen.id());

        boolean loteOrigenVacio = !lotes.hasActiveAnimals(loteOrigen.id(), user.empresaId());
        int permanecen = lotes.findMemberships(loteOrigen.id(), user.empresaId(), true).size();
        return new ResultadoMovimientoLote(guardado.id(), loteOrigen.id(), loteDestinoResultante,
                animalesAMover.size(), permanecen, loteOrigenVacio, esTransferenciaCompleta, tipo);
    }

    @Transactional
    public void cancelar(UUID preparacionId) {
        CurrentUser user = context.requirePermission("LOTE_MOVER");
        PreparacionMovimientoLote prep = requirePreparacion(preparacionId);
        requireLote(prep.loteOrigenId(), user.empresaId());
        if (prep.estado() != EstadoPreparacionLote.VIGENTE) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_ESTADO_INVALIDO);
        }
        preparaciones.marcarEstado(preparacionId, EstadoPreparacionLote.CANCELADA, prep.version(), user.userId());
        audit(user, "CANCELAR_PREPARACION_MOVIMIENTO_LOTE", prep.loteOrigenId());
    }

    /**
     * Anula un "Mover lote" ya confirmado: nunca se borra el movimiento original, se revierte con
     * {@link MovimientoService#revert} (que ya crea el movimiento compensatorio inverso, restaura
     * la ubicación/membresía de cada animal y rechaza la reversión si algún animal ya no está
     * exactamente donde este movimiento lo dejó — p. ej. porque un movimiento posterior lo movió
     * de nuevo). Sobre eso, esta capa agrega dos correcciones específicas de "Mover lote" que la
     * reversión genérica no conoce: (1) bloquea si algún animal ya no está ACTIVO (vendido, muerto,
     * descartado) o si el lote creado en la misma operación (CREAR_NUEVO_LOTE) recibió después
     * miembros ajenos a ella; (2) recalcula la proyección potrero_actual_id de los lotes
     * involucrados y, si la operación había transferido la propiedad del lote completo
     * (MANTENER_LOTE + cambio de propiedad), revierte también esa transferencia.
     */
    @Transactional
    public Movimiento anular(UUID preparacionId, String motivo) {
        CurrentUser user = context.requirePermission("LOTE_MOVER");
        PreparacionMovimientoLote prep = requirePreparacion(preparacionId);
        Lote loteOrigen = requireLote(prep.loteOrigenId(), user.empresaId());
        context.requirePropertyAccess(user, loteOrigen.propiedadId());
        if (prep.estado() != EstadoPreparacionLote.CONFIRMADA || prep.movimientoResultanteId() == null) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_ESTADO_INVALIDO,
                    "Solo se puede anular una preparación ya confirmada.");
        }
        Movimiento original = movimientos.findById(prep.movimientoResultanteId(), user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PREPARACION_LOTE_NOT_FOUND));

        List<MovimientoDetalle> detalles = movimientos.findDetalles(original.id());
        for (MovimientoDetalle d : detalles) {
            Animal animal = animales.findById(d.animalId(), user.empresaId()).orElse(null);
            if (animal == null || animal.estado() != EstadoAnimal.ACTIVO) {
                throw new BusinessException(ErrorCode.PREPARACION_LOTE_ANULACION_BLOQUEADA,
                        "El animal " + d.animalId() + " ya no está activo; anule o compense manualmente.");
            }
        }
        if (prep.loteResultanteId() != null && !prep.loteResultanteId().equals(prep.loteOrigenId())) {
            long miembrosActuales = lotes.findMemberships(prep.loteResultanteId(), user.empresaId(), true).size();
            if (miembrosActuales != detalles.size()) {
                throw new BusinessException(ErrorCode.PREPARACION_LOTE_ANULACION_BLOQUEADA,
                        "El lote creado en esta operación ya tiene otros movimientos posteriores.");
            }
        }

        Movimiento revertido = movimientoService.revert(original.id(), motivo, original.version());

        lotes.recomputarUbicacionOperativa(prep.loteOrigenId(), user.userId());
        if (prep.loteResultanteId() != null && !prep.loteResultanteId().equals(prep.loteOrigenId())) {
            lotes.recomputarUbicacionOperativa(prep.loteResultanteId(), user.userId());
        }
        if (original.tipo() == TipoMovimiento.TRANSFERENCIA_PROPIEDAD
                && original.origenLoteId() != null && original.origenLoteId().equals(original.destinoLoteId())) {
            lotes.transferirPropiedad(original.origenLoteId(), original.origenPropiedadId(), user.userId());
        }
        audit(user, "ANULAR_MOVIMIENTO_LOTE", prep.loteOrigenId());
        return revertido;
    }

    private void validarDestinoBasico(CurrentUser user, PrepararMovimientoLoteCommand cmd) {
        if (cmd.destinoPropiedadId() == null || cmd.destinoPotreroId() == null) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_DESTINO_INVALIDO,
                    "Indica la propiedad y el potrero de destino.");
        }
        if (!animales.validLocation(user.empresaId(), cmd.destinoPropiedadId(), cmd.destinoPotreroId())) {
            throw new BusinessException(ErrorCode.PREPARACION_LOTE_DESTINO_INVALIDO,
                    "El potrero de destino no pertenece a la propiedad de destino.");
        }
        if (cmd.accionLote() == null) throw new BusinessException(ErrorCode.PREPARACION_LOTE_ACCION_INVALIDA);
        switch (cmd.accionLote()) {
            case CAMBIAR_A_LOTE_EXISTENTE -> {
                if (cmd.loteDestinoId() == null) {
                    throw new BusinessException(ErrorCode.PREPARACION_LOTE_DESTINO_INVALIDO, "Indica el lote de destino.");
                }
                Lote destino = lotes.findById(cmd.loteDestinoId(), user.empresaId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
                if (destino.estado() != EstadoLote.ACTIVO) throw new BusinessException(ErrorCode.LOT_CLOSED);
                if (!destino.propiedadId().equals(cmd.destinoPropiedadId())) {
                    throw new BusinessException(ErrorCode.PREPARACION_LOTE_DESTINO_INVALIDO,
                            "El lote de destino no pertenece a la propiedad de destino.");
                }
            }
            case CREAR_NUEVO_LOTE -> {
                if (cmd.nuevoLote() == null || cmd.nuevoLote().nombre() == null || cmd.nuevoLote().nombre().isBlank()) {
                    throw new BusinessException(ErrorCode.PREPARACION_LOTE_NUEVO_LOTE_DATOS_REQUERIDOS);
                }
            }
            case MANTENER_LOTE, DEJAR_SIN_LOTE -> { }
        }
    }

    private Elegibilidad evaluarElegibilidad(Animal animal, Lote loteOrigen, CurrentUser user) {
        if (animal.estado() != EstadoAnimal.ACTIVO) {
            return Elegibilidad.excluido(switch (animal.estado()) {
                case VENDIDO -> "El animal ya fue vendido.";
                case MUERTO -> "El animal está registrado como muerto.";
                case PERDIDO -> "El animal está registrado como perdido.";
                case DESCARTADO -> "El animal fue descartado.";
                case TRANSFERIDO -> "El animal ya fue transferido.";
                case ACTIVO -> throw new IllegalStateException();
            });
        }
        if (!Objects.equals(animal.propiedadActualId(), loteOrigen.propiedadId())) {
            return Elegibilidad.excluido("La propiedad actual del animal no coincide con la propiedad del lote.");
        }
        if (animal.potreroActualId() == null
                || !animales.validLocation(user.empresaId(), animal.propiedadActualId(), animal.potreroActualId())) {
            return Elegibilidad.excluido("La ubicación actual del animal es inconsistente.");
        }
        if (movimientos.existsPendientePorAnimal(animal.id(), user.empresaId())) {
            return Elegibilidad.excluido("Tiene un movimiento pendiente sin confirmar.");
        }
        boolean enCuarentena = movimientos.findUltimoConfirmadoPorAnimal(animal.id(), user.empresaId())
                .map(m -> m.tipo() == TipoMovimiento.CUARENTENA).orElse(false);
        if (enCuarentena) {
            return Elegibilidad.excluido("El animal está en cuarentena activa.");
        }
        List<RestriccionSanitaria> restricciones = new ArrayList<>();
        RestriccionSanitariaPort puerto = restriccionSanitaria == null ? null : restriccionSanitaria.getIfAvailable();
        if (puerto != null) restricciones.addAll(puerto.evaluar(user.empresaId(), animal.id()));
        Optional<RestriccionSanitaria> bloqueante = restricciones.stream()
                .filter(r -> r.severidad() == SeveridadRestriccion.BLOQUEANTE).findFirst();
        if (bloqueante.isPresent()) {
            return Elegibilidad.excluidoConRestricciones(bloqueante.get().mensaje(), restricciones);
        }
        return Elegibilidad.elegible(restricciones);
    }

    private TipoMovimiento determinarTipo(Lote loteOrigen, PreparacionMovimientoLote prep, UUID loteDestinoResultante) {
        if (!prep.destinoPropiedadId().equals(loteOrigen.propiedadId())) return TipoMovimiento.TRANSFERENCIA_PROPIEDAD;
        UUID origenPotrero = prep.potreroOrigenId();
        if (origenPotrero != null && !origenPotrero.equals(prep.destinoPotreroId())) return TipoMovimiento.CAMBIO_POTRERO;
        boolean loteCambia = !Objects.equals(loteDestinoResultante, loteOrigen.id());
        if (loteCambia) return TipoMovimiento.CAMBIO_LOTE;
        return TipoMovimiento.CAMBIO_POTRERO;
    }

    private String motivoMovimiento(PreparacionMovimientoLote prep, TipoMovimiento tipo) {
        return "Movimiento de lote (" + tipo.name() + ")" + (prep.motivo() == null || prep.motivo().isBlank() ? "" : ": " + prep.motivo());
    }

    private void publicarEventoAnimal(CurrentUser user, Animal animal, UUID loteAntes, UUID loteDespues,
                                      TipoMovimiento tipo, UUID movimientoId, UUID preparacionId,
                                      PreparacionMovimientoLote prep) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tipo", tipo.name());
        metadata.put("preparacionId", preparacionId.toString());
        metadata.put("loteOrigenId", prep.loteOrigenId().toString());
        if (loteDespues != null) metadata.put("loteDestinoId", loteDespues.toString());
        metadata.put("destinoPropiedadId", prep.destinoPropiedadId().toString());
        metadata.put("destinoPotreroId", prep.destinoPotreroId().toString());
        TipoEventoAnimal tipoEvento = loteDespues == null ? TipoEventoAnimal.LOTE_REMOVIDO
                : (Objects.equals(loteAntes, loteDespues) ? TipoEventoAnimal.MOVIMIENTO_REGISTRADO : TipoEventoAnimal.LOTE_CAMBIADO);
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), animal.id(), tipoEvento, null,
                prep.motivo(), null, movimientoId, metadata, user.userId(), Instant.now(), null));
    }

    private PreparacionResultado armarResultado(PreparacionMovimientoLote prep, List<PreparacionMovimientoLoteMiembro> miembros) {
        int elegibles = (int) miembros.stream().filter(PreparacionMovimientoLoteMiembro::elegible).count();
        int total = miembros.size();
        return new PreparacionResultado(prep, miembros, total, elegibles, total - elegibles);
    }

    private Lote requireLote(UUID id, UUID empresa) {
        return lotes.findById(id, empresa).orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
    }

    private PreparacionMovimientoLote requirePreparacion(UUID id) {
        return preparaciones.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PREPARACION_LOTE_NOT_FOUND));
    }

    private void audit(CurrentUser user, String accion, UUID id) {
        events.publishEvent(new MoverLoteAuditEvent(user.empresaId(), user.userId(), accion, id, Instant.now()));
    }

    private record Elegibilidad(boolean elegible, String motivoExclusion, List<RestriccionSanitaria> restricciones) {
        static Elegibilidad excluido(String motivo) {
            return new Elegibilidad(false, motivo, List.of());
        }

        static Elegibilidad excluidoConRestricciones(String motivo, List<RestriccionSanitaria> restricciones) {
            return new Elegibilidad(false, motivo, restricciones);
        }

        static Elegibilidad elegible(List<RestriccionSanitaria> restricciones) {
            return new Elegibilidad(true, null, restricciones);
        }
    }
}
