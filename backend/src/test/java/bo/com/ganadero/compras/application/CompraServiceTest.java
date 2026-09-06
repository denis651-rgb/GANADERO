package bo.com.ganadero.compras.application;

import bo.com.ganadero.animales.application.AnimalService;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.compras.domain.*;
import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.pesajes.domain.TipoPeso;
import bo.com.ganadero.proveedores.application.ProveedorService;
import bo.com.ganadero.proveedores.domain.Proveedor;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CompraServiceTest {
    private CompraRepository compras;
    private ProveedorService proveedores;
    private AnimalService animales;
    private MovimientoService movimientos;
    private PesajeRepository pesajes;
    private CodigoService codigos;
    private CompraService service;
    private UUID propiedad, potrero, raza, proveedorId, actorId;

    @BeforeEach
    void setUp() {
        compras = mock(CompraRepository.class);
        proveedores = mock(ProveedorService.class);
        animales = mock(AnimalService.class);
        movimientos = mock(MovimientoService.class);
        pesajes = mock(PesajeRepository.class);
        codigos = mock(CodigoService.class);
        propiedad = UUID.randomUUID();
        potrero = UUID.randomUUID();
        raza = UUID.randomUUID();
        proveedorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(actorId, UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                Set.of("COMPRA_CREAR", "COMPRA_EDITAR", "COMPRA_CONFIRMAR", "COMPRA_ANULAR", "COMPRA_VER"), Set.of(), true);
        UserContext context = new UserContext(() -> user);
        service = new CompraService(compras, proveedores, animales, movimientos, pesajes, context, codigos,
                mock(ApplicationEventPublisher.class));

        when(codigos.paraCreacion(any(), eq(TipoCodigo.COMPRA), any(), any(), any())).thenReturn("COM-000001");
        when(proveedores.buscarOCrear(any(), any())).thenAnswer(inv -> proveedor());
        when(compras.crear(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(compras.actualizarBorrador(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Proveedor proveedor() {
        return new Proveedor(proveedorId, "Estancia El Roble", "76543210", "1234567", null, null, null, true,
                Instant.now(), actorId, Instant.now(), actorId, 0);
    }

    private CompraDetalleCommand detalle(BigDecimal override, BigDecimal peso, TipoPeso tipoPeso) {
        return new CompraDetalleCommand(null, "Animal", SexoAnimal.HEMBRA, raza, PropositoAnimal.CARNE, null, null,
                18, bo.com.ganadero.animales.domain.UnidadEdadDeclarada.MESES, java.time.LocalDate.now(),
                bo.com.ganadero.animales.domain.FuenteEdadDeclarada.PROVEEDOR, null, null, null, override, peso,
                tipoPeso, null, null, null, null, null);
    }

    private CompraCommand comando(ModalidadPrecio modalidad, BigDecimal unitario, BigDecimal total,
                                  List<CompraDetalleCommand> detalles) {
        return new CompraCommand(proveedorId, null, Instant.now(), modalidad, "BOB", unitario, total, propiedad,
                potrero, null, PropositoAnimal.CARNE, null, detalles);
    }

    @Test
    void porUnidadCalculaTotalComoSumaDeDetalles() {
        var cmd = comando(ModalidadPrecio.POR_UNIDAD, new BigDecimal("100"), null,
                List.of(detalle(null, null, null), detalle(null, null, null), detalle(null, null, null)));

        Compra creada = service.crearBorrador(cmd);

        assertThat(creada.precioTotal()).isEqualByComparingTo("300");
        assertThat(creada.precioUnitarioReferencial()).isEqualByComparingTo("100.00");
        assertThat(creada.cantidadAnimales()).isEqualTo(3);
    }

    @Test
    void porTropaDistribuyeUniformeYElUltimoAbsorbeElRemanente() {
        var cmd = comando(ModalidadPrecio.POR_TROPA, null, new BigDecimal("100"),
                List.of(detalle(null, null, null), detalle(null, null, null), detalle(null, null, null)));

        ArgumentCaptor<List<CompraDetalle>> captor = ArgumentCaptor.forClass(List.class);
        service.crearBorrador(cmd);
        verify(compras).crear(any(), captor.capture(), any());

        List<CompraDetalle> detalles = captor.getValue();
        assertThat(detalles).extracting(CompraDetalle::precioAsignado)
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        BigDecimal suma = detalles.stream().map(CompraDetalle::precioAsignado).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo("100");
    }

    @Test
    void porTropaConAjustesQueNoSumanElTotalSeRechaza() {
        var cmd = comando(ModalidadPrecio.POR_TROPA, null, new BigDecimal("100"), List.of(
                detalle(new BigDecimal("40"), null, null), detalle(new BigDecimal("40"), null, null)));

        assertThatThrownBy(() -> service.crearBorrador(cmd))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.COMPRA_TOTAL_INCONSISTENTE));
    }

    @Test
    void porTropaConAjusteParcialSeRechaza() {
        var cmd = comando(ModalidadPrecio.POR_TROPA, null, new BigDecimal("100"), List.of(
                detalle(new BigDecimal("60"), null, null), detalle(null, null, null)));

        assertThatThrownBy(() -> service.crearBorrador(cmd)).isInstanceOf(BusinessException.class);
    }

    @Test
    void precioNegativoSeRechaza() {
        var cmd = comando(ModalidadPrecio.POR_UNIDAD, new BigDecimal("-5"), null, List.of(detalle(null, null, null)));

        assertThatThrownBy(() -> service.crearBorrador(cmd))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.COMPRA_PRECIO_INVALIDO));
    }

    @Test
    void pesoSinTipoSeRechaza() {
        var cmd = comando(ModalidadPrecio.POR_UNIDAD, new BigDecimal("100"), null,
                List.of(detalle(null, new BigDecimal("150"), null)));

        assertThatThrownBy(() -> service.crearBorrador(cmd)).isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmarYaConfirmadaEsIdempotenteYNoRecreaAnimales() {
        Compra confirmada = compraConEstado(EstadoCompra.CONFIRMADA);
        when(compras.findById(confirmada.id())).thenReturn(java.util.Optional.of(confirmada));

        Compra resultado = service.confirmar(confirmada.id(), confirmada.version());

        assertThat(resultado).isEqualTo(confirmada);
        verify(animales, never()).create(any());
        verify(movimientos, never()).create(any());
    }

    @Test
    void confirmarCreaUnAnimalUnMovimientoYUnPesajePorDetalleConPeso() {
        Compra borrador = compraConEstado(EstadoCompra.BORRADOR);
        CompraDetalle d1 = detalleGuardado(borrador.id(), 1, new BigDecimal("150"), TipoPeso.MEDIDO);
        CompraDetalle d2 = detalleGuardado(borrador.id(), 2, null, null);
        when(compras.findById(borrador.id())).thenReturn(java.util.Optional.of(borrador));
        when(compras.findDetalles(borrador.id())).thenReturn(List.of(d1, d2));

        Animal animal1 = animal();
        Animal animal2 = animal();
        when(animales.create(any())).thenReturn(animal1, animal2);
        Movimiento movimiento = movimiento();
        when(movimientos.create(any())).thenReturn(movimiento);
        when(movimientos.confirm(any(), anyLong())).thenReturn(movimiento);
        Compra confirmadaFinal = compraConEstado(EstadoCompra.CONFIRMADA);
        when(compras.confirmar(eq(borrador.id()), anyLong(), any())).thenReturn(confirmadaFinal);

        Compra resultado = service.confirmar(borrador.id(), borrador.version());

        assertThat(resultado.estado()).isEqualTo(EstadoCompra.CONFIRMADA);
        verify(animales, times(2)).create(any());
        verify(movimientos).create(any());
        verify(movimientos).confirm(movimiento.id(), movimiento.version());
        verify(pesajes, times(1)).create(any(Pesaje.class), any());
        verify(compras).asignarAnimal(d1.id(), animal1.id());
        verify(compras).asignarAnimal(d2.id(), animal2.id());
        verify(compras).confirmar(borrador.id(), borrador.version(), actorId);
    }

    @Test
    void siUnDetalleFallaNoSeConfirmaLaCompra() {
        Compra borrador = compraConEstado(EstadoCompra.BORRADOR);
        CompraDetalle d1 = detalleGuardado(borrador.id(), 1, null, null);
        when(compras.findById(borrador.id())).thenReturn(java.util.Optional.of(borrador));
        when(compras.findDetalles(borrador.id())).thenReturn(List.of(d1));
        when(animales.create(any())).thenThrow(new BusinessException(ErrorCode.BREED_NOT_FOUND));

        assertThatThrownBy(() -> service.confirmar(borrador.id(), borrador.version()))
                .isInstanceOf(BusinessException.class);
        verify(compras, never()).confirmar(any(), anyLong(), any());
        verify(movimientos, never()).create(any());
    }

    @Test
    void anularSinDependenciasDescartaLosAnimales() {
        Compra confirmada = compraConEstado(EstadoCompra.CONFIRMADA);
        UUID animalId = UUID.randomUUID();
        CompraDetalle d1 = detalleConAnimal(confirmada.id(), animalId);
        when(compras.findById(confirmada.id())).thenReturn(java.util.Optional.of(confirmada));
        when(compras.findDetalles(confirmada.id())).thenReturn(List.of(d1));
        when(compras.dependenciasPosteriores(animalId, confirmada.id())).thenReturn(List.of());
        Animal animal = animalConEstado(animalId, EstadoAnimal.ACTIVO);
        when(animales.get(animalId)).thenReturn(animal);
        when(compras.anular(eq(confirmada.id()), any(), anyLong(), any())).thenReturn(compraConEstado(EstadoCompra.ANULADA));

        service.anular(confirmada.id(), "Proveedor no entregó documentación", confirmada.version());

        verify(animales).changeState(eq(animalId), eq(EstadoAnimal.DESCARTADO), contains("Compra anulada"), eq(animal.version()));
        verify(compras).anular(eq(confirmada.id()), any(), anyLong(), any());
    }

    @Test
    void anularConDependenciasBloquea() {
        Compra confirmada = compraConEstado(EstadoCompra.CONFIRMADA);
        UUID animalId = UUID.randomUUID();
        CompraDetalle d1 = detalleConAnimal(confirmada.id(), animalId);
        when(compras.findById(confirmada.id())).thenReturn(java.util.Optional.of(confirmada));
        when(compras.findDetalles(confirmada.id())).thenReturn(List.of(d1));
        when(compras.dependenciasPosteriores(animalId, confirmada.id()))
                .thenReturn(List.of(new DependenciaCompra("A-001", "Venta registrada", "1 registro(s)")));

        assertThatThrownBy(() -> service.anular(confirmada.id(), "motivo", confirmada.version()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.COMPRA_TIENE_DEPENDENCIAS));
        verify(animales, never()).changeState(any(), any(), any(), anyLong());
        verify(compras, never()).anular(any(), any(), anyLong(), any());
    }

    @Test
    void anularSinMotivoSeRechaza() {
        Compra confirmada = compraConEstado(EstadoCompra.CONFIRMADA);
        assertThatThrownBy(() -> service.anular(confirmada.id(), " ", confirmada.version()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.COMPRA_MOTIVO_REQUERIDO));
    }

    private Compra compraConEstado(EstadoCompra estado) {
        return new Compra(UUID.randomUUID(), "COM-000001", proveedorId, Instant.now(), ModalidadPrecio.POR_UNIDAD,
                "BOB", 1, new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), propiedad, potrero,
                null, PropositoAnimal.CARNE, null, estado, null, null, null, null, Instant.now(), actorId,
                Instant.now(), actorId, 0);
    }

    private CompraDetalle detalleGuardado(UUID compraId, int linea, BigDecimal peso, TipoPeso tipoPeso) {
        return new CompraDetalle(UUID.randomUUID(), compraId, null, linea, new BigDecimal("100"), peso, tipoPeso,
                null, propiedad, potrero, null, null, "Animal", SexoAnimal.HEMBRA, raza, PropositoAnimal.CARNE,
                null, false, null, null, null, null, null, null, null, null, Instant.now());
    }

    private CompraDetalle detalleConAnimal(UUID compraId, UUID animalId) {
        return new CompraDetalle(UUID.randomUUID(), compraId, animalId, 1, new BigDecimal("100"), null, null,
                null, propiedad, potrero, null, null, "Animal", SexoAnimal.HEMBRA, raza, PropositoAnimal.CARNE,
                null, false, null, null, null, null, null, null, null, null, Instant.now());
    }

    private Animal animal() {
        return animalConEstado(UUID.randomUUID(), EstadoAnimal.ACTIVO);
    }

    private Animal animalConEstado(UUID id, EstadoAnimal estado) {
        return new Animal(id, UUID.randomUUID(), "ANI-00000" + id.toString().charAt(0), null, SexoAnimal.HEMBRA,
                null, false, raza, UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, propiedad,
                potrero, null, estado, java.time.LocalDate.now(), new BigDecimal("100"), null, null, null, null, 0);
    }

    private Movimiento movimiento() {
        return new Movimiento(UUID.randomUUID(), UUID.randomUUID(), TipoMovimiento.INGRESO_COMPRA,
                EstadoMovimiento.CONFIRMADO, java.time.LocalDate.now(), null, null, null, null, null,
                propiedad, potrero, null, actorId, actorId, null, Instant.now(), null, null, null, null, null,
                null, null, 0);
    }
}
