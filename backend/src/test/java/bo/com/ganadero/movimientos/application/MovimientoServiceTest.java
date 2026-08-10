package bo.com.ganadero.movimientos.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.MovimientoAnimal;
import bo.com.ganadero.movimientos.domain.MovimientoDetalle;
import bo.com.ganadero.movimientos.domain.MovimientoRepository;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovimientoServiceTest {
    private MovimientoRepository movimientos;
    private AnimalRepository animales;
    private LoteRepository lotes;
    private UUID company;
    private UUID property;
    private UUID otherProperty;
    private UUID paddock;
    private UUID destinoPaddock;
    private UUID loteId;
    private UUID destinoLote;
    private UUID animalId;
    private UUID otherAnimalId;
    private UUID movId;
    private UUID userId;
    private final List<Object> published = new ArrayList<>();
    private MovimientoService service;

    @BeforeEach
    void setup() {
        published.clear();
        movimientos = mock(MovimientoRepository.class);
        animales = mock(AnimalRepository.class);
        lotes = mock(LoteRepository.class);
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        otherProperty = UUID.randomUUID();
        paddock = UUID.randomUUID();
        destinoPaddock = UUID.randomUUID();
        loteId = UUID.randomUUID();
        destinoLote = UUID.randomUUID();
        animalId = UUID.randomUUID();
        otherAnimalId = UUID.randomUUID();
        movId = UUID.randomUUID();
        userId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, company, UUID.randomUUID(), Set.of(),
                Set.of("MOVIMIENTO_VER", "MOVIMIENTO_CREAR", "MOVIMIENTO_CONFIRMAR",
                        "MOVIMIENTO_ANULAR", "MOVIMIENTO_REVERTIR"),
                Set.of(), true);
        service = new MovimientoService(movimientos, animales, lotes,
                new UserContext(() -> user), published::add, published::add);
    }

    @Test
    void createCambioPotreroCreaMovimientoPendiente() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(movimientos.create(any(Movimiento.class), any(), eq(userId)))
                .thenReturn(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0));
        Movimiento created = service.create(comando(TipoMovimiento.CAMBIO_POTRERO, property, paddock, null,
                null, destinoPaddock, null, List.of(new MovimientoAnimal(animalId, 0))));
        assertThat(created.estado()).isEqualTo(EstadoMovimiento.PENDIENTE);
        verify(movimientos).create(any(Movimiento.class), any(), eq(userId));
        assertThat(published).anyMatch(e -> e instanceof MovimientoAuditEvent ev && "CREAR".equals(ev.accion()));
    }

    @Test
    void createCambioLoteRechazaSinLoteDestino() {
        assertThatThrownBy(() -> service.create(comando(TipoMovimiento.CAMBIO_LOTE, property, paddock, loteId,
                null, null, null, List.of(new MovimientoAnimal(animalId, 0)))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_MOVEMENT_DESTINATION));
        verify(movimientos, never()).create(any(), any(), any());
    }

    @Test
    void createRechazaAnimalesDuplicados() {
        assertThatThrownBy(() -> service.create(comando(TipoMovimiento.CAMBIO_POTRERO, property, paddock, null,
                null, destinoPaddock, null,
                List.of(new MovimientoAnimal(animalId, 0), new MovimientoAnimal(animalId, 0)))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.DUPLICATE_ANIMAL_IN_MOVEMENT));
    }

    @Test
    void createRechazaOrigenIncorrecto() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, destinoPaddock, null, 0)));
        assertThatThrownBy(() -> service.create(comando(TipoMovimiento.CAMBIO_POTRERO, property, paddock, null,
                null, destinoPaddock, null, List.of(new MovimientoAnimal(animalId, 0)))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_MOVEMENT_ORIGIN));
    }

    @Test
    void createRechazaDestinoFueraDePropiedad() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(animales.validLocation(company, otherProperty, destinoPaddock)).thenReturn(false);
        assertThatThrownBy(() -> service.create(comando(TipoMovimiento.CAMBIO_POTRERO, property, paddock, null,
                otherProperty, destinoPaddock, null, List.of(new MovimientoAnimal(animalId, 0)))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_MOVEMENT_DESTINATION));
    }

    @Test
    void confirmCambioPotreroMueveAnimalYPublicaTimeline() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, property, destinoPaddock, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(animales.validLocation(company, property, destinoPaddock)).thenReturn(true);
        when(movimientos.confirm(eq(movId), eq(company), eq(0L), eq(userId)))
                .thenReturn(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 1));
        Movimiento confirmed = service.confirm(movId, 0);
        assertThat(confirmed.estado()).isEqualTo(EstadoMovimiento.CONFIRMADO);
        verify(animales).move(animalId, company, property, destinoPaddock, null, userId);
        verify(movimientos).saveDetalleUbicaciones(eq(movId), any());
        assertThat(published).anyMatch(e -> e instanceof RegistrarEventoTimeline ev
                && ev.tipo() == TipoEventoAnimal.MOVIMIENTO_REGISTRADO && ev.animalId().equals(animalId));
        assertThat(published).anyMatch(e -> e instanceof MovimientoAuditEvent ev && "CONFIRMAR".equals(ev.accion()));
    }

    @Test
    void confirmCambioLoteCierraYReabreMembresia() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_LOTE,
                        property, paddock, loteId, null, null, destinoLote, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, loteId, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, loteId, 0)));
        when(lotes.findById(destinoLote, company)).thenReturn(Optional.of(lote(destinoLote, EstadoLote.ACTIVO)));
        when(lotes.findActiveLotOfAnimal(animalId, company)).thenReturn(Optional.of(lote(loteId, EstadoLote.ACTIVO)));
        when(movimientos.confirm(eq(movId), eq(company), eq(0L), eq(userId)))
                .thenReturn(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_LOTE,
                        property, paddock, loteId, null, null, destinoLote, 1));
        service.confirm(movId, 0);
        verify(lotes).closeMembership(eq(loteId), eq(null), eq(animalId), eq(company),
                eq("Movimiento CAMBIO_LOTE"), any(Instant.class), eq(userId));
        verify(lotes).openMembership(eq(destinoLote), any(), eq(animalId), eq(company),
                eq("Movimiento CAMBIO_LOTE"), any(), eq("PARCIAL"), any(Instant.class), eq(userId));
        verify(animales).move(animalId, company, property, paddock, destinoLote, userId);
    }

    @Test
    void confirmTransferenciaMueveAnimalAPropiedadDestino() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.TRANSFERENCIA_PROPIEDAD,
                        property, paddock, null, otherProperty, null, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(movimientos.confirm(eq(movId), eq(company), eq(0L), eq(userId)))
                .thenReturn(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.TRANSFERENCIA_PROPIEDAD,
                        property, paddock, null, otherProperty, null, null, 1));
        service.confirm(movId, 0);
        verify(animales).move(animalId, company, otherProperty, paddock, null, userId);
    }

    @Test
    void confirmCuarentenaPublicaEventoCuarentena() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CUARENTENA,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(animales.validLocation(company, property, destinoPaddock)).thenReturn(true);
        when(movimientos.confirm(any(), any(), anyLong(), any()))
                .thenReturn(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.CUARENTENA,
                        property, paddock, null, null, destinoPaddock, null, 1));
        service.confirm(movId, 0);
        assertThat(published).anyMatch(e -> e instanceof RegistrarEventoTimeline ev
                && ev.tipo() == TipoEventoAnimal.CUARENTENA_INICIADA && ev.animalId().equals(animalId));
    }

    @Test
    void confirmRechazaAnimalMuerto() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.MUERTO, property, paddock, null, 0)));
        assertThatThrownBy(() -> service.confirm(movId, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED));
        verify(animales, never()).move(any(), any(), any(), any(), any(), any());
    }

    @Test
    void validarReportaAnimalVendidoComoInvalido() {
        when(movimientos.findById(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.VENDIDO, property, paddock, null, 0)));
        ValidacionMovimiento resultado = service.validar(movId);
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.invalidos()).isEqualTo(1);
        assertThat(resultado.resultados()).singleElement().satisfies(r -> {
            assertThat(r.valido()).isFalse();
            assertThat(r.error()).isEqualTo(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED);
        });
    }

    @Test
    void confirmRechazaPotreroDestinoIgualOrigen() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, paddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        assertThatThrownBy(() -> service.confirm(movId, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_MOVEMENT_DESTINATION));
        verify(animales, never()).move(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmRechazaSinPermisoSobrePropiedadDestino() {
        CurrentUser limited = new CurrentUser(userId, company, UUID.randomUUID(), Set.of(),
                Set.of("MOVIMIENTO_VER", "MOVIMIENTO_CONFIRMAR"), Set.of(property), false);
        MovimientoService limitedService = new MovimientoService(movimientos, animales, lotes,
                new UserContext(() -> limited), published::add, published::add);
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.TRANSFERENCIA_PROPIEDAD,
                        property, paddock, null, otherProperty, null, null, 0)));
        assertThatThrownBy(() -> limitedService.confirm(movId, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PROPERTY_ACCESS_DENIED));
        verify(animales, never()).move(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmSegundaVezRechazaYaConfirmado() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 1)));
        assertThatThrownBy(() -> service.confirm(movId, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.MOVEMENT_ALREADY_CONFIRMED));
        verify(animales, never()).move(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmVersionIncorrectaPropagaConflicto() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(animales.validLocation(company, property, destinoPaddock)).thenReturn(true);
        when(movimientos.confirm(eq(movId), eq(company), eq(0L), eq(userId)))
                .thenThrow(new BusinessException(ErrorCode.MOVEMENT_VERSION_CONFLICT));
        assertThatThrownBy(() -> service.confirm(movId, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.MOVEMENT_VERSION_CONFLICT));
    }

    @Test
    void confirmVariosAnimalesSiUnoFallaNoMueveNada() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(
                        detalle(animalId, 0, property, paddock, null, null, null, null),
                        detalle(otherAnimalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(animales.findByIdForUpdate(otherAnimalId, company))
                .thenReturn(Optional.of(animal(otherAnimalId, EstadoAnimal.VENDIDO, property, paddock, null, 0)));
        assertThatThrownBy(() -> service.confirm(movId, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED));
        verify(animales, never()).move(any(), any(), any(), any(), any(), any());
        verify(movimientos, never()).confirm(any(), any(), anyLong(), any());
    }

    @Test
    void confirmSalidaVentaMarcaVendido() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.SALIDA_VENTA,
                        property, paddock, null, otherProperty, null, null, 0)));
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, null, null, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null, 0)));
        when(movimientos.confirm(any(), any(), anyLong(), any()))
                .thenReturn(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.SALIDA_VENTA,
                        property, paddock, null, otherProperty, null, null, 1));
        service.confirm(movId, 0);
        verify(animales).changeState(eq(animalId), eq(company), eq(EstadoAnimal.ACTIVO),
                eq(EstadoAnimal.VENDIDO), anyString(), anyLong(), eq(userId));
        verify(animales).move(eq(animalId), eq(company), eq(otherProperty), eq(paddock), eq(null), eq(userId));
    }

    @Test
    void revertCambioPotreroRestauraUbicacion() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 1)));
        when(movimientos.findByOriginal(movId, company)).thenReturn(Optional.empty());
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, null, property, destinoPaddock, null)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, destinoPaddock, null, 1)));
        when(movimientos.markReverted(any(), any(), any(), any(), anyLong(), any()))
                .thenReturn(movimiento(EstadoMovimiento.REVERTIDO, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 2));
        service.revert(movId, "Se revierte", 1);
        verify(animales).restoreLocation(animalId, company, property, paddock, null, userId);
        verify(movimientos).saveConfirmed(any(Movimiento.class), any(), eq(userId));
        verify(movimientos).markReverted(eq(movId), eq(company), any(UUID.class), eq("Se revierte"),
                eq(1L), eq(userId));
        assertThat(published).anyMatch(e -> e instanceof RegistrarEventoTimeline ev
                && ev.tipo() == TipoEventoAnimal.MOVIMIENTO_REVERTIDO && ev.animalId().equals(animalId));
        assertThat(published).anyMatch(e -> e instanceof MovimientoAuditEvent ev && "REVERTIR".equals(ev.accion()));
    }

    @Test
    void revertCambioLoteRestauraMembresia() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_LOTE,
                        property, paddock, loteId, null, null, destinoLote, 1)));
        when(movimientos.findByOriginal(movId, company)).thenReturn(Optional.empty());
        when(movimientos.findDetalles(movId))
                .thenReturn(List.of(detalle(animalId, 0, property, paddock, loteId, property, paddock, destinoLote)));
        when(animales.findByIdForUpdate(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, destinoLote, 1)));
        when(lotes.findActiveLotOfAnimal(animalId, company))
                .thenReturn(Optional.of(lote(destinoLote, EstadoLote.ACTIVO)));
        when(movimientos.markReverted(any(), any(), any(), any(), anyLong(), any()))
                .thenReturn(movimiento(EstadoMovimiento.REVERTIDO, TipoMovimiento.CAMBIO_LOTE,
                        property, paddock, loteId, null, null, destinoLote, 2));
        service.revert(movId, "Corrección", 1);
        verify(lotes).closeMembership(eq(destinoLote), eq(null), eq(animalId), eq(company),
                anyString(), any(Instant.class), eq(userId));
        verify(lotes).openMembership(eq(loteId), eq(null), eq(animalId), eq(company),
                anyString(), any(), eq("PARCIAL"), any(Instant.class), eq(userId));
        verify(animales).restoreLocation(animalId, company, property, paddock, loteId, userId);
    }

    @Test
    void revertRechazaMovimientoYaRevertido() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.REVERTIDO, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 2)));
        assertThatThrownBy(() -> service.revert(movId, "x", 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.MOVEMENT_ALREADY_REVERTED));
        verify(animales, never()).restoreLocation(any(), any(), any(), any(), any(), any());
    }

    @Test
    void annulMovimientoPendiente() {
        when(movimientos.findByIdForUpdate(movId, company))
                .thenReturn(Optional.of(movimiento(EstadoMovimiento.PENDIENTE, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 0)));
        when(movimientos.annul(eq(movId), eq(company), eq("Me equivoqué"), eq(0L), eq(userId)))
                .thenReturn(movimiento(EstadoMovimiento.ANULADO, TipoMovimiento.CAMBIO_POTRERO,
                        property, paddock, null, null, destinoPaddock, null, 1));
        Movimiento anulado = service.annul(movId, "Me equivoqué", 0);
        assertThat(anulado.estado()).isEqualTo(EstadoMovimiento.ANULADO);
        assertThat(published).anyMatch(e -> e instanceof MovimientoAuditEvent ev && "ANULAR".equals(ev.accion()));
    }

    private MovimientoCommand comando(TipoMovimiento tipo, UUID origProp, UUID origPotrero, UUID origLote,
                                      UUID destProp, UUID destPotrero, UUID destLote,
                                      List<MovimientoAnimal> animales) {
        return new MovimientoCommand(null, tipo, LocalDate.now(), "Motivo de prueba", null,
                origProp, origPotrero, origLote, destProp, destPotrero, destLote, animales);
    }

    private Movimiento movimiento(EstadoMovimiento estado, TipoMovimiento tipo, UUID origProp, UUID origPotrero,
                                  UUID origLote, UUID destProp, UUID destPotrero, UUID destLote, long version) {
        return new Movimiento(movId, company, tipo, estado, LocalDate.now(), "Motivo de prueba", null,
                origProp, origPotrero, origLote, destProp, destPotrero, destLote,
                userId, null, null, null, null, null, null, null, null, null, null, version);
    }

    private MovimientoDetalle detalle(UUID animal, long esperada, UUID propAntes, UUID potreroAntes, UUID loteAntes,
                                      UUID propDespues, UUID potreroDespues, UUID loteDespues) {
        return new MovimientoDetalle(UUID.randomUUID(), movId, animal, esperada,
                EstadoAnimal.ACTIVO, EstadoAnimal.ACTIVO,
                propAntes, potreroAntes, loteAntes, propDespues, potreroDespues, loteDespues,
                "PENDIENTE", null);
    }

    private Animal animal(UUID id, EstadoAnimal state, UUID prop, UUID potrero, UUID lote, long version) {
        return new Animal(id, company, "A-1", null, SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO,
                prop, potrero, lote, state, LocalDate.now(), null, null, null, null, null, version);
    }

    private Lote lote(UUID id, EstadoLote estado) {
        return new Lote(id, company, property, "L-1", "Lote 1", null, estado, LocalDate.now(), null, 0);
    }
}
