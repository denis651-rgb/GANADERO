package bo.com.ganadero.movimientolote.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.movimientolote.domain.*;
import bo.com.ganadero.movimientos.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MoverLoteServiceTest {
    private PreparacionMovimientoLoteRepository preparaciones;
    private LoteRepository lotes;
    private AnimalRepository animales;
    private MovimientoRepository movimientos;
    private bo.com.ganadero.movimientos.application.MovimientoService movimientoService;
    private CodigoService codigos;
    @SuppressWarnings("unchecked")
    private final ObjectProvider<RestriccionSanitariaPort> restriccionProvider = mock(ObjectProvider.class);
    private MoverLoteService service;

    private UUID empresa, loteId, propiedadOrigen, potreroOrigen, propiedadDestino, potreroDestino, actorId;
    private CurrentUser user;

    @BeforeEach
    void setUp() {
        preparaciones = mock(PreparacionMovimientoLoteRepository.class);
        lotes = mock(LoteRepository.class);
        animales = mock(AnimalRepository.class);
        movimientos = mock(MovimientoRepository.class);
        movimientoService = mock(bo.com.ganadero.movimientos.application.MovimientoService.class);
        codigos = mock(CodigoService.class);
        empresa = UUID.randomUUID();
        loteId = UUID.randomUUID();
        propiedadOrigen = UUID.randomUUID();
        potreroOrigen = UUID.randomUUID();
        propiedadDestino = UUID.randomUUID();
        potreroDestino = UUID.randomUUID();
        actorId = UUID.randomUUID();
        user = new CurrentUser(actorId, empresa, UUID.randomUUID(), Set.of(), Set.of("LOTE_MOVER"), Set.of(), true);
        UserContext context = new UserContext(() -> user);
        service = new MoverLoteService(preparaciones, lotes, animales, movimientos, movimientoService, context, codigos,
                mock(ApplicationEventPublisher.class), evento -> { }, restriccionProvider);

        when(preparaciones.crear(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(animales.validLocation(eq(empresa), any(), any())).thenReturn(true);
        when(movimientos.existsPendientePorAnimal(any(), eq(empresa))).thenReturn(false);
        when(movimientos.findUltimoConfirmadoPorAnimal(any(), eq(empresa))).thenReturn(Optional.empty());
        when(restriccionProvider.getIfAvailable()).thenReturn(null);
    }

    private Lote lote() {
        return new Lote(loteId, empresa, propiedadOrigen, "LOT-001", "Lote A", null, EstadoLote.ACTIVO,
                LocalDate.now(), null, 0, null, 1, potreroOrigen);
    }

    private Animal animal(UUID id, EstadoAnimal estado, long version) {
        return new Animal(id, empresa, "ANI-" + id.toString().substring(0, 4), null, SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO,
                propiedadOrigen, potreroOrigen, loteId, estado, LocalDate.now(), null, null, null, null, null, version);
    }

    private MembresiaLote membresia(UUID animalId) {
        return new MembresiaLote(UUID.randomUUID(), loteId, animalId, Instant.now().minusSeconds(3600), null,
                "INGRESO_COMPRA", null, null, "PARCIAL", actorId, null, 0);
    }

    private PrepararMovimientoLoteCommand prepararCommand(AccionLote accion, UUID destinoPropiedad, UUID destinoPotrero) {
        return new PrepararMovimientoLoteCommand(destinoPropiedad, destinoPotrero, accion, null, null, null,
                "Rotación de potrero", null, ModalidadMovimientoLote.LOTE_COMPLETO);
    }

    // ---------- preparar(): elegibilidad ----------

    @Test
    void excluyeAnimalesVendidosMuertosYDescartados() {
        UUID vendido = UUID.randomUUID();
        UUID muerto = UUID.randomUUID();
        UUID activo = UUID.randomUUID();
        when(lotes.findMemberships(loteId, empresa, true))
                .thenReturn(List.of(membresia(vendido), membresia(muerto), membresia(activo)));
        when(animales.findById(vendido, empresa)).thenReturn(Optional.of(animal(vendido, EstadoAnimal.VENDIDO, 0)));
        when(animales.findById(muerto, empresa)).thenReturn(Optional.of(animal(muerto, EstadoAnimal.MUERTO, 0)));
        when(animales.findById(activo, empresa)).thenReturn(Optional.of(animal(activo, EstadoAnimal.ACTIVO, 0)));

        PreparacionResultado resultado = service.preparar(loteId(), prepararCommand(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino));

        assertThat(resultado.totalEncontrados()).isEqualTo(3);
        assertThat(resultado.totalElegibles()).isEqualTo(1);
        assertThat(resultado.totalExcluidos()).isEqualTo(2);
        assertThat(miembro(resultado, vendido).motivoExclusion()).containsIgnoringCase("vendido");
        assertThat(miembro(resultado, muerto).motivoExclusion()).containsIgnoringCase("muerto");
        assertThat(miembro(resultado, activo).elegible()).isTrue();
    }

    @Test
    void excluyePorCuarentenaActiva() {
        UUID enCuarentena = UUID.randomUUID();
        when(lotes.findMemberships(loteId, empresa, true)).thenReturn(List.of(membresia(enCuarentena)));
        when(animales.findById(enCuarentena, empresa)).thenReturn(Optional.of(animal(enCuarentena, EstadoAnimal.ACTIVO, 0)));
        Movimiento cuarentena = movimientoCuarentena(TipoMovimiento.CUARENTENA);
        when(movimientos.findUltimoConfirmadoPorAnimal(enCuarentena, empresa)).thenReturn(Optional.of(cuarentena));

        PreparacionResultado resultado = service.preparar(loteId(), prepararCommand(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino));

        assertThat(miembro(resultado, enCuarentena).elegible()).isFalse();
        assertThat(miembro(resultado, enCuarentena).motivoExclusion()).containsIgnoringCase("cuarentena");
    }

    @Test
    void excluyePorRestriccionBloqueanteYConservaAdvertenciaEnElegible() {
        UUID critico = UUID.randomUUID();
        UUID conAdvertencia = UUID.randomUUID();
        when(lotes.findMemberships(loteId, empresa, true))
                .thenReturn(List.of(membresia(critico), membresia(conAdvertencia)));
        when(animales.findById(critico, empresa)).thenReturn(Optional.of(animal(critico, EstadoAnimal.ACTIVO, 0)));
        when(animales.findById(conAdvertencia, empresa)).thenReturn(Optional.of(animal(conAdvertencia, EstadoAnimal.ACTIVO, 0)));
        RestriccionSanitariaPort puerto = mock(RestriccionSanitariaPort.class);
        when(restriccionProvider.getIfAvailable()).thenReturn(puerto);
        when(puerto.evaluar(empresa, critico)).thenReturn(List.of(
                new RestriccionSanitaria("CASO_CLINICO_CRITICO", SeveridadRestriccion.BLOQUEANTE, "Aislamiento requerido.")));
        when(puerto.evaluar(empresa, conAdvertencia)).thenReturn(List.of(
                new RestriccionSanitaria("TRATAMIENTO_ACTIVO", SeveridadRestriccion.ADVERTENCIA, "Tratamiento activo.")));

        PreparacionResultado resultado = service.preparar(loteId(), prepararCommand(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino));

        assertThat(miembro(resultado, critico).elegible()).isFalse();
        assertThat(miembro(resultado, conAdvertencia).elegible()).isTrue();
        assertThat(miembro(resultado, conAdvertencia).restricciones()).hasSize(1);
    }

    // ---------- confirmar() ----------

    @Test
    void confirmaCambioDePotreroDentroDeLaMismaPropiedad() {
        UUID animalId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino, List.of(animalId), 1);
        prepararMocksConfirmar(prep, List.of(animalId));

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        ResultadoMovimientoLote resultado = service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId)));

        verify(movimientos).saveConfirmed(captor.capture(), any(), any());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoMovimiento.CAMBIO_POTRERO);
        assertThat(resultado.identidadTransferida()).isTrue();
        verify(lotes, never()).transferirPropiedad(any(), any(), any());
        verify(lotes).recomputarUbicacionOperativa(loteId, actorId);
    }

    @Test
    void confirmaTransferenciaDePropiedadCompletaYTransfiereIdentidadDelLote() {
        UUID animalId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadDestino, potreroDestino, List.of(animalId), 1);
        prepararMocksConfirmar(prep, List.of(animalId));

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId)));

        verify(movimientos).saveConfirmed(captor.capture(), any(), any());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoMovimiento.TRANSFERENCIA_PROPIEDAD);
        verify(lotes).transferirPropiedad(loteId, propiedadDestino, actorId);
    }

    @Test
    void rechazaMantenerLoteEnMovimientoParcial() {
        UUID seleccionado = UUID.randomUUID();
        UUID noSeleccionado = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino,
                List.of(seleccionado, noSeleccionado), 2);
        prepararMocksConfirmar(prep, List.of(seleccionado, noSeleccionado));

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(seleccionado))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_ACCION_INVALIDA));
        verify(movimientos, never()).saveConfirmed(any(), any(), any());
    }

    @Test
    void confirmaCambioALoteExistente() {
        UUID animalId = UUID.randomUUID();
        UUID loteDestinoId = UUID.randomUUID();
        Lote loteDestino = new Lote(loteDestinoId, empresa, propiedadDestino, "LOT-002", "Lote B", null,
                EstadoLote.ACTIVO, LocalDate.now(), null, 0);
        when(lotes.findById(loteDestinoId, empresa)).thenReturn(Optional.of(loteDestino));

        PreparacionMovimientoLote prep = new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen,
                potreroOrigen, ModalidadMovimientoLote.LOTE_COMPLETO, propiedadDestino, potreroDestino,
                AccionLote.CAMBIAR_A_LOTE_EXISTENTE, loteDestinoId, null, null, null, Instant.now().minusSeconds(5),
                "motivo", null, EstadoPreparacionLote.VIGENTE, Instant.now(), Instant.now().plusSeconds(600),
                null, null, actorId, 0);
        prepararMocksConfirmar(prep, List.of(animalId));

        ResultadoMovimientoLote resultado = service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId)));

        verify(lotes).openMembership(eq(loteDestinoId), isNull(), eq(animalId), eq(empresa), any(), any(), any(), any(), eq(actorId));
        verify(lotes).closeMembership(eq(loteId), isNull(), eq(animalId), eq(empresa), any(), any(), eq(actorId));
        assertThat(resultado.loteDestinoId()).isEqualTo(loteDestinoId);
    }

    @Test
    void reportaAnimalesQuePermanecenConUnaRelecturaFrescaDelLoteOrigenNoConLaFotografiaCongelada() {
        UUID seleccionado = UUID.randomUUID();
        UUID noSeleccionado = UUID.randomUUID();
        UUID loteDestinoId = UUID.randomUUID();
        Lote loteDestino = new Lote(loteDestinoId, empresa, propiedadOrigen, "LOT-002", "Lote B", null,
                EstadoLote.ACTIVO, LocalDate.now(), null, 0);
        when(lotes.findById(loteDestinoId, empresa)).thenReturn(Optional.of(loteDestino));
        PreparacionMovimientoLote prep = new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen,
                potreroOrigen, ModalidadMovimientoLote.SELECCION_PARCIAL, propiedadOrigen, potreroDestino,
                AccionLote.CAMBIAR_A_LOTE_EXISTENTE, loteDestinoId, null, null, null, Instant.now().minusSeconds(5),
                "motivo", null, EstadoPreparacionLote.VIGENTE, Instant.now(), Instant.now().plusSeconds(600),
                null, null, actorId, 0);
        prepararMocksConfirmar(prep, List.of(seleccionado, noSeleccionado));
        // Simula que, para cuando se recalcula el resultado, otro proceso ya sacó también a un
        // tercer animal del lote origen: una relectura fresca debe reflejarlo, la fotografía no.
        when(lotes.findMemberships(loteId, empresa, true)).thenReturn(List.of(membresia(noSeleccionado)));

        ResultadoMovimientoLote resultado = service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(seleccionado)));

        assertThat(resultado.animalesMovidos()).isEqualTo(1);
        assertThat(resultado.animalesPermanecenEnOrigen()).isEqualTo(1);
    }

    @Test
    void confirmaCreacionDeNuevoLoteDentroDeLaTransaccion() {
        UUID animalId = UUID.randomUUID();
        UUID nuevoLoteId = UUID.randomUUID();
        when(codigos.paraCreacion(eq(user), eq(TipoCodigo.LOTE), isNull(), anyInt(), any())).thenReturn("LOT-003");
        Lote creado = new Lote(nuevoLoteId, empresa, propiedadDestino, "LOT-003", "Lote nuevo", null,
                EstadoLote.ACTIVO, LocalDate.now(), null, 0);
        when(lotes.create(any(), eq(actorId))).thenReturn(creado);

        PreparacionMovimientoLote prep = new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen,
                potreroOrigen, ModalidadMovimientoLote.LOTE_COMPLETO, propiedadDestino, potreroDestino,
                AccionLote.CREAR_NUEVO_LOTE, null, "Lote nuevo", null, null, Instant.now().minusSeconds(5),
                "motivo", null, EstadoPreparacionLote.VIGENTE, Instant.now(), Instant.now().plusSeconds(600),
                null, null, actorId, 0);
        prepararMocksConfirmar(prep, List.of(animalId));

        ResultadoMovimientoLote resultado = service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId)));

        verify(lotes).create(any(), eq(actorId));
        verify(preparaciones).confirmar(eq(prep.id()), any(), eq(nuevoLoteId), eq(prep.version()), eq(actorId));
        assertThat(resultado.loteDestinoId()).isEqualTo(nuevoLoteId);
    }

    @Test
    void confirmaDejarAnimalesSinLote() {
        UUID animalId = UUID.randomUUID();
        PreparacionMovimientoLote prep = new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen,
                potreroOrigen, ModalidadMovimientoLote.LOTE_COMPLETO, propiedadOrigen, potreroDestino,
                AccionLote.DEJAR_SIN_LOTE, null, null, null, null, Instant.now().minusSeconds(5),
                "motivo", null, EstadoPreparacionLote.VIGENTE, Instant.now(), Instant.now().plusSeconds(600),
                null, null, actorId, 0);
        prepararMocksConfirmar(prep, List.of(animalId));

        service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId)));

        verify(lotes).closeMembership(eq(loteId), isNull(), eq(animalId), eq(empresa), any(), any(), eq(actorId));
        verify(lotes, never()).openMembership(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(animales).move(animalId, empresa, propiedadOrigen, potreroDestino, null, actorId);
    }

    @Test
    void rechazaSeleccionQueNoPerteneceALaFotografia() {
        UUID fotografiado = UUID.randomUUID();
        UUID ajeno = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino, List.of(fotografiado), 1);
        prepararMocksConfirmar(prep, List.of(fotografiado));

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(ajeno))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_SELECCION_INVALIDA));
        verify(movimientos, never()).saveConfirmed(any(), any(), any());
    }

    @Test
    void detectaAnimalModificadoDesdeLaPreparacionYNoMueveNada() {
        UUID animalId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino, List.of(animalId), 1);
        prepararMocksConfirmar(prep, List.of(animalId));
        // el animal real ya tiene version 5, pero la fotografia capturo version 0
        when(animales.findByIdForUpdate(animalId, empresa)).thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, 5)));

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_ANIMAL_CAMBIO));
        verify(movimientos, never()).saveConfirmed(any(), any(), any());
        verify(animales, never()).move(any(), any(), any(), any(), any(), any());
    }

    @Test
    void detectaAnimalQueYaNoPerteneceAlLoteOrigen() {
        UUID animalId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino, List.of(animalId), 1);
        prepararMocksConfirmar(prep, List.of(animalId));
        when(lotes.findActiveMembership(animalId, empresa)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_ANIMAL_CAMBIO));
        verify(movimientos, never()).saveConfirmed(any(), any(), any());
    }

    @Test
    void bloqueaAdvertenciaSanitariaSinAutorizarYPermiteConAutorizacion() {
        UUID animalId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino, List.of(animalId), 1);
        prepararMocksConfirmar(prep, List.of(animalId));
        RestriccionSanitariaPort puerto = mock(RestriccionSanitariaPort.class);
        when(restriccionProvider.getIfAvailable()).thenReturn(puerto);
        when(puerto.evaluar(empresa, animalId)).thenReturn(List.of(
                new RestriccionSanitaria("TRATAMIENTO_ACTIVO", SeveridadRestriccion.ADVERTENCIA, "Tratamiento activo.")));

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(animalId))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_ANIMAL_CAMBIO));

        ConfirmarMovimientoLoteCommand conAutorizacion = new ConfirmarMovimientoLoteCommand(prep.version(), List.of(animalId),
                List.of(new AutorizacionCommand(animalId, "TRATAMIENTO_ACTIVO", "Autorizado por el encargado")));
        service.confirmar(prep.id(), conAutorizacion);
        verify(movimientos).saveConfirmed(any(), any(), any());
        verify(preparaciones).registrarAutorizacion(prep.id(), animalId, "TRATAMIENTO_ACTIVO", "Autorizado por el encargado", actorId);
    }

    // ---------- anular() ----------

    private PreparacionMovimientoLote preparacionConfirmada(UUID movimientoResultanteId, UUID loteResultanteId) {
        return new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen, potreroOrigen,
                ModalidadMovimientoLote.LOTE_COMPLETO, propiedadOrigen, potreroDestino, AccionLote.MANTENER_LOTE,
                null, null, null, null, Instant.now().minusSeconds(600), "motivo", null,
                EstadoPreparacionLote.CONFIRMADA, Instant.now().minusSeconds(600), Instant.now().minusSeconds(300),
                movimientoResultanteId, loteResultanteId, actorId, 0);
    }

    @Test
    void anulaUnMovimientoConfirmadoYRevierteCadaAnimalYRecalculaUbicacion() {
        UUID animalId = UUID.randomUUID();
        UUID movimientoId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionConfirmada(movimientoId, loteId);
        when(preparaciones.findById(prep.id())).thenReturn(Optional.of(prep));
        when(lotes.findById(loteId, empresa)).thenReturn(Optional.of(lote()));
        Movimiento original = new Movimiento(movimientoId, empresa, TipoMovimiento.CAMBIO_POTRERO,
                EstadoMovimiento.CONFIRMADO, LocalDate.now(), "motivo", null, propiedadOrigen, potreroOrigen, loteId,
                propiedadOrigen, potreroDestino, loteId, actorId, actorId, null, Instant.now(), null, null, null,
                null, null, null, null, 3);
        when(movimientos.findById(movimientoId, empresa)).thenReturn(Optional.of(original));
        when(movimientos.findDetalles(movimientoId)).thenReturn(List.of(
                new MovimientoDetalle(UUID.randomUUID(), movimientoId, animalId, 0, EstadoAnimal.ACTIVO,
                        EstadoAnimal.ACTIVO, propiedadOrigen, potreroOrigen, loteId, propiedadOrigen, potreroDestino,
                        loteId, "OK", null)));
        when(animales.findById(animalId, empresa)).thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, 1)));
        Movimiento revertido = new Movimiento(movimientoId, empresa, TipoMovimiento.CAMBIO_POTRERO,
                EstadoMovimiento.REVERTIDO, LocalDate.now(), "motivo", null, propiedadOrigen, potreroOrigen, loteId,
                propiedadOrigen, potreroDestino, loteId, actorId, actorId, null, Instant.now(), null, null, actorId,
                Instant.now(), "ya no aplica", null, UUID.randomUUID(), 4);
        when(movimientoService.revert(movimientoId, "ya no aplica", 3)).thenReturn(revertido);

        Movimiento resultado = service.anular(prep.id(), "ya no aplica");

        assertThat(resultado.estado()).isEqualTo(EstadoMovimiento.REVERTIDO);
        verify(movimientoService).revert(movimientoId, "ya no aplica", 3);
        verify(lotes).recomputarUbicacionOperativa(loteId, actorId);
        verify(lotes, never()).transferirPropiedad(any(), any(), any());
    }

    @Test
    void bloqueaLaAnulacionSiUnAnimalYaNoEstaActivo() {
        UUID animalId = UUID.randomUUID();
        UUID movimientoId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionConfirmada(movimientoId, loteId);
        when(preparaciones.findById(prep.id())).thenReturn(Optional.of(prep));
        when(lotes.findById(loteId, empresa)).thenReturn(Optional.of(lote()));
        Movimiento original = new Movimiento(movimientoId, empresa, TipoMovimiento.CAMBIO_POTRERO,
                EstadoMovimiento.CONFIRMADO, LocalDate.now(), "motivo", null, propiedadOrigen, potreroOrigen, loteId,
                propiedadOrigen, potreroDestino, loteId, actorId, actorId, null, Instant.now(), null, null, null,
                null, null, null, null, 3);
        when(movimientos.findById(movimientoId, empresa)).thenReturn(Optional.of(original));
        when(movimientos.findDetalles(movimientoId)).thenReturn(List.of(
                new MovimientoDetalle(UUID.randomUUID(), movimientoId, animalId, 0, EstadoAnimal.ACTIVO,
                        EstadoAnimal.ACTIVO, propiedadOrigen, potreroOrigen, loteId, propiedadOrigen, potreroDestino,
                        loteId, "OK", null)));
        when(animales.findById(animalId, empresa)).thenReturn(Optional.of(animal(animalId, EstadoAnimal.VENDIDO, 1)));

        assertThatThrownBy(() -> service.anular(prep.id(), "motivo"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_ANULACION_BLOQUEADA));
        verify(movimientoService, never()).revert(any(), any(), anyLong());
    }

    @Test
    void bloqueaLaAnulacionSiElLoteCreadoRecibioMiembrosPosteriores() {
        UUID animalId = UUID.randomUUID();
        UUID movimientoId = UUID.randomUUID();
        UUID loteNuevoId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionConfirmada(movimientoId, loteNuevoId);
        when(preparaciones.findById(prep.id())).thenReturn(Optional.of(prep));
        when(lotes.findById(loteId, empresa)).thenReturn(Optional.of(lote()));
        Movimiento original = new Movimiento(movimientoId, empresa, TipoMovimiento.CAMBIO_LOTE,
                EstadoMovimiento.CONFIRMADO, LocalDate.now(), "motivo", null, propiedadOrigen, potreroOrigen, loteId,
                propiedadOrigen, potreroDestino, loteNuevoId, actorId, actorId, null, Instant.now(), null, null, null,
                null, null, null, null, 3);
        when(movimientos.findById(movimientoId, empresa)).thenReturn(Optional.of(original));
        when(movimientos.findDetalles(movimientoId)).thenReturn(List.of(
                new MovimientoDetalle(UUID.randomUUID(), movimientoId, animalId, 0, EstadoAnimal.ACTIVO,
                        EstadoAnimal.ACTIVO, propiedadOrigen, potreroOrigen, loteId, propiedadOrigen, potreroDestino,
                        loteNuevoId, "OK", null)));
        when(animales.findById(animalId, empresa)).thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, 1)));
        // el lote nuevo ya tiene 2 miembros activos, pero esta operación solo trajo a 1
        when(lotes.findMemberships(loteNuevoId, empresa, true)).thenReturn(List.of(membresia(animalId), membresia(UUID.randomUUID())));

        assertThatThrownBy(() -> service.anular(prep.id(), "motivo"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_ANULACION_BLOQUEADA));
        verify(movimientoService, never()).revert(any(), any(), anyLong());
    }

    @Test
    void revierteLaTransferenciaDePropiedadCompletaAlAnular() {
        UUID animalId = UUID.randomUUID();
        UUID movimientoId = UUID.randomUUID();
        PreparacionMovimientoLote prep = preparacionConfirmada(movimientoId, loteId);
        when(preparaciones.findById(prep.id())).thenReturn(Optional.of(prep));
        when(lotes.findById(loteId, empresa)).thenReturn(Optional.of(lote()));
        Movimiento original = new Movimiento(movimientoId, empresa, TipoMovimiento.TRANSFERENCIA_PROPIEDAD,
                EstadoMovimiento.CONFIRMADO, LocalDate.now(), "motivo", null, propiedadOrigen, potreroOrigen, loteId,
                propiedadDestino, potreroDestino, loteId, actorId, actorId, null, Instant.now(), null, null, null,
                null, null, null, null, 3);
        when(movimientos.findById(movimientoId, empresa)).thenReturn(Optional.of(original));
        when(movimientos.findDetalles(movimientoId)).thenReturn(List.of(
                new MovimientoDetalle(UUID.randomUUID(), movimientoId, animalId, 0, EstadoAnimal.ACTIVO,
                        EstadoAnimal.ACTIVO, propiedadOrigen, potreroOrigen, loteId, propiedadDestino, potreroDestino,
                        loteId, "OK", null)));
        when(animales.findById(animalId, empresa)).thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, 1)));
        when(movimientoService.revert(eq(movimientoId), any(), eq(3L))).thenReturn(original);

        service.anular(prep.id(), "motivo");

        verify(lotes).transferirPropiedad(loteId, propiedadOrigen, actorId);
    }

    @Test
    void rechazaPreparacionExpirada() {
        PreparacionMovimientoLote prep = new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen,
                potreroOrigen, ModalidadMovimientoLote.LOTE_COMPLETO, propiedadOrigen, potreroDestino,
                AccionLote.MANTENER_LOTE, null, null, null, null, Instant.now(),
                "motivo", null, EstadoPreparacionLote.VIGENTE, Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1), null, null, actorId, 0);
        when(preparaciones.findByIdForUpdate(prep.id())).thenReturn(Optional.of(prep));

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version(), List.of(UUID.randomUUID()))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PREPARACION_LOTE_EXPIRADA));
        verify(preparaciones).marcarEstado(prep.id(), EstadoPreparacionLote.EXPIRADA, prep.version(), actorId);
    }

    @Test
    void rechazaVersionDesactualizada() {
        PreparacionMovimientoLote prep = preparacionVigente(AccionLote.MANTENER_LOTE, propiedadOrigen, potreroDestino,
                List.of(UUID.randomUUID()), 1);
        when(preparaciones.findByIdForUpdate(prep.id())).thenReturn(Optional.of(prep));

        assertThatThrownBy(() -> service.confirmar(prep.id(), confirmarCommand(prep.version() + 1, List.of(UUID.randomUUID()))))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    // ---------- helpers ----------

    private UUID loteId() {
        when(lotes.findById(loteId, empresa)).thenReturn(Optional.of(lote()));
        return loteId;
    }

    private PreparacionMovimientoLote preparacionVigente(AccionLote accion, UUID destinoPropiedad, UUID destinoPotrero,
                                                          List<UUID> elegibles, int totalElegibles) {
        return new PreparacionMovimientoLote(UUID.randomUUID(), loteId, propiedadOrigen, potreroOrigen,
                totalElegibles > 1 ? ModalidadMovimientoLote.SELECCION_PARCIAL : ModalidadMovimientoLote.LOTE_COMPLETO,
                destinoPropiedad, destinoPotrero, accion, null, null, null, null, Instant.now().minusSeconds(5),
                "motivo", null, EstadoPreparacionLote.VIGENTE, Instant.now(), Instant.now().plusSeconds(600),
                null, null, actorId, 0);
    }

    /** Registra los mocks comunes para que confirmar() encuentre la preparación, el lote de origen,
     * la fotografía (todos elegibles y seleccionados) y animales frescos válidos para cada id dado. */
    private void prepararMocksConfirmar(PreparacionMovimientoLote prep, List<UUID> animalIds) {
        when(preparaciones.findByIdForUpdate(prep.id())).thenReturn(Optional.of(prep));
        when(lotes.findById(loteId, empresa)).thenReturn(Optional.of(lote()));
        List<PreparacionMovimientoLoteMiembro> miembros = animalIds.stream()
                .map(id -> new PreparacionMovimientoLoteMiembro(UUID.randomUUID(), prep.id(), id, "ANI", null,
                        "ACTIVO", propiedadOrigen, potreroOrigen, loteId, 0, true, null, true, List.of(), Instant.now()))
                .toList();
        when(preparaciones.findMiembros(prep.id())).thenReturn(miembros);
        for (UUID id : animalIds) {
            Animal fresh = animal(id, EstadoAnimal.ACTIVO, 0);
            when(animales.findByIdForUpdate(id, empresa)).thenReturn(Optional.of(fresh));
            when(lotes.findActiveMembership(id, empresa)).thenReturn(Optional.of(membresia(id)));
        }
        Movimiento guardado = new Movimiento(UUID.randomUUID(), empresa, TipoMovimiento.CAMBIO_POTRERO,
                EstadoMovimiento.CONFIRMADO, LocalDate.now(), null, null, propiedadOrigen, potreroOrigen, loteId,
                prep.destinoPropiedadId(), prep.destinoPotreroId(), null, actorId, actorId, null, Instant.now(),
                null, null, null, null, null, null, null, 0);
        when(movimientos.saveConfirmed(any(), any(), any())).thenReturn(guardado);
        List<MovimientoDetalle> detalles = animalIds.stream()
                .map(id -> new MovimientoDetalle(UUID.randomUUID(), guardado.id(), id, 0, EstadoAnimal.ACTIVO,
                        EstadoAnimal.ACTIVO, null, null, null, null, null, null, null, null))
                .toList();
        when(movimientos.findDetalles(guardado.id())).thenReturn(detalles);
        when(preparaciones.confirmar(eq(prep.id()), any(), any(), eq(prep.version()), eq(actorId))).thenReturn(prep);
        when(lotes.hasActiveAnimals(any(), eq(empresa))).thenReturn(false);
    }

    private ConfirmarMovimientoLoteCommand confirmarCommand(long version, List<UUID> animalIds) {
        return new ConfirmarMovimientoLoteCommand(version, animalIds, List.of());
    }

    private Movimiento movimientoCuarentena(TipoMovimiento tipo) {
        return new Movimiento(UUID.randomUUID(), empresa, tipo, EstadoMovimiento.CONFIRMADO, LocalDate.now(), null,
                null, propiedadOrigen, potreroOrigen, loteId, propiedadOrigen, UUID.randomUUID(), loteId,
                actorId, actorId, null, Instant.now(), null, null, null, null, null, null, null, 0);
    }

    private PreparacionMovimientoLoteMiembro miembro(PreparacionResultado resultado, UUID animalId) {
        return resultado.miembros().stream().filter(m -> animalId.equals(m.animalId())).findFirst().orElseThrow();
    }
}
