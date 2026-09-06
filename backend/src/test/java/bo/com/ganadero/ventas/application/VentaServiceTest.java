package bo.com.ganadero.ventas.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import bo.com.ganadero.pesajes.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.ventas.domain.ModalidadVenta;
import bo.com.ganadero.ventas.domain.Venta;
import bo.com.ganadero.ventas.domain.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VentaServiceTest {
    private VentaRepository ventas;
    private AnimalRepository animales;
    private MovimientoService movimientos;
    private PesajeRepository pesajes;
    private VentaService service;
    private UUID empresa, animalId, propiedad, potrero;

    @BeforeEach
    void setUp() {
        ventas = mock(VentaRepository.class);
        animales = mock(AnimalRepository.class);
        movimientos = mock(MovimientoService.class);
        pesajes = mock(PesajeRepository.class);
        empresa = UUID.randomUUID();
        animalId = UUID.randomUUID();
        propiedad = UUID.randomUUID();
        potrero = UUID.randomUUID();
        CurrentUser user = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of(),
                Set.of("VENTA_REGISTRAR", "VENTA_VER"), Set.of(), true);
        @SuppressWarnings("unchecked")
        ObjectProvider<RestriccionRetiroPort> sinRetiro = mock(ObjectProvider.class);
        service = new VentaService(ventas, animales, movimientos, pesajes, new UserContext(() -> user), sinRetiro);

        Movimiento movimiento = new Movimiento(UUID.randomUUID(), empresa, TipoMovimiento.SALIDA_VENTA,
                EstadoMovimiento.CONFIRMADO, LocalDate.now(), null, null, null, null, null, null, null, null,
                UUID.randomUUID(), UUID.randomUUID(), null, Instant.now(), null, null, null, null, null, null, null, 0);
        when(movimientos.create(any())).thenReturn(movimiento);
        when(movimientos.confirm(any(), anyLong())).thenReturn(movimiento);
        when(ventas.create(any())).thenAnswer(inv -> inv.getArgument(0));
        when(animales.findById(animalId, empresa)).thenReturn(Optional.of(animal()));
    }

    private Animal animal() {
        return new Animal(animalId, empresa, "ANI-000001", null, SexoAnimal.HEMBRA, null, false, UUID.randomUUID(),
                UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, propiedad, potrero, null,
                EstadoAnimal.ACTIVO, LocalDate.now(), null, null, null, null, null, 3);
    }

    private Animal animal(UUID id, String codigo) {
        return new Animal(id, empresa, codigo, null, SexoAnimal.HEMBRA, null, false, UUID.randomUUID(),
                UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, propiedad, potrero, null,
                EstadoAnimal.ACTIVO, LocalDate.now(), null, null, null, null, null, 3);
    }

    private Pesaje pesajeActivo(BigDecimal peso) {
        return new Pesaje(UUID.randomUUID(), empresa, animalId, LocalDate.now().minusDays(2), peso, TipoPesaje.RUTINA,
                TipoPeso.MEDIDO, null, null, null, propiedad, potrero, null, null, null, null, null, null, null,
                EstadoPesaje.ACTIVO, null, null, null, null, null, null, null, null, null, null, 0);
    }

    @Test
    void usaElPesajeExistenteSinDuplicarlo() {
        Pesaje existente = pesajeActivo(new BigDecimal("380"));
        when(pesajes.findById(existente.id(), empresa)).thenReturn(Optional.of(existente));

        Venta venta = service.registrar(new VentaCommand(animalId, LocalDate.now(), "Frigorífico Norte",
                new BigDecimal("5000"), "BOB", null, null, existente.id(), null, null));

        assertThat(venta.pesoVentaKg()).isEqualByComparingTo("380");
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void creaUnPesajeNuevoConMotivoVentaEnlazadoALaVenta() {
        Venta venta = service.registrar(new VentaCommand(animalId, LocalDate.now(), "Frigorífico Norte",
                new BigDecimal("5000"), "BOB", new BigDecimal("400"), null, null, TipoPeso.ESTIMADO, "BASCULA"));

        assertThat(venta.pesoVentaKg()).isEqualByComparingTo("400");
        ArgumentCaptor<Pesaje> captor = ArgumentCaptor.forClass(Pesaje.class);
        verify(pesajes).create(captor.capture(), any());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoPesaje.VENTA);
        assertThat(captor.getValue().tipoPeso()).isEqualTo(TipoPeso.ESTIMADO);
        assertThat(captor.getValue().ventaId()).isEqualTo(venta.id());
    }

    @Test
    void rechazaUnPesajeQueNoPerteneceAlAnimal() {
        UUID otroAnimal = UUID.randomUUID();
        Pesaje deOtroAnimal = new Pesaje(UUID.randomUUID(), empresa, otroAnimal, LocalDate.now(), new BigDecimal("300"),
                TipoPesaje.RUTINA, TipoPeso.MEDIDO, null, null, null, propiedad, potrero, null, null, null, null,
                null, null, null, EstadoPesaje.ACTIVO, null, null, null, null, null, null, null, null, null, null, 0);
        when(pesajes.findById(deOtroAnimal.id(), empresa)).thenReturn(Optional.of(deOtroAnimal));

        assertThatThrownBy(() -> service.registrar(new VentaCommand(animalId, LocalDate.now(), "Comprador",
                new BigDecimal("100"), "BOB", null, null, deOtroAnimal.id(), null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VENTA_PESAJE_INVALIDO));
    }

    @Test
    void rechazaPrecioInvalido() {
        assertThatThrownBy(() -> service.registrar(new VentaCommand(animalId, LocalDate.now(), "Comprador",
                BigDecimal.ZERO, "BOB", null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VENTA_PRECIO_INVALIDO));
    }

    @Test
    void bloqueaLaVentaSiHayUnRetiroSanitarioVigente() {
        @SuppressWarnings("unchecked")
        ObjectProvider<RestriccionRetiroPort> conRetiro = mock(ObjectProvider.class);
        RestriccionRetiroPort port = mock(RestriccionRetiroPort.class);
        when(conRetiro.getIfAvailable()).thenReturn(port);
        when(port.vigente(eq(empresa), eq(animalId), any())).thenReturn(
                Optional.of(new RestriccionRetiroPort.RestriccionRetiroVigente("CARNE", LocalDate.now().plusDays(5))));
        CurrentUser user = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of(),
                Set.of("VENTA_REGISTRAR", "VENTA_VER"), Set.of(), true);
        VentaService conBloqueo = new VentaService(ventas, animales, movimientos, pesajes,
                new UserContext(() -> user), conRetiro);

        assertThatThrownBy(() -> conBloqueo.registrar(new VentaCommand(animalId, LocalDate.now(), "Comprador",
                new BigDecimal("100"), "BOB", null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VENTA_RETIRO_SANITARIO_VIGENTE));
        verify(movimientos, never()).create(any());
    }

    @Test
    void ventaPorLoteEnPieAplicaElMismoPrecioACadaAnimal() {
        UUID animalId2 = UUID.randomUUID();
        when(animales.findById(animalId2, empresa)).thenReturn(Optional.of(animal(animalId2, "ANI-000002")));

        List<Venta> ventas = service.registrarLote(new VentaLoteCommand(List.of(animalId, animalId2),
                LocalDate.now(), "Frigorífico Norte", "77712345", ModalidadVenta.EN_PIE,
                new BigDecimal("3500"), null, Map.of(), null));

        assertThat(ventas).hasSize(2);
        assertThat(ventas).allSatisfy(v -> {
            assertThat(v.precio()).isEqualByComparingTo("3500");
            assertThat(v.modalidad()).isEqualTo(ModalidadVenta.EN_PIE);
            assertThat(v.telefonoComprador()).isEqualTo("77712345");
        });
        assertThat(ventas.get(0).grupoVentaId()).isNotNull().isEqualTo(ventas.get(1).grupoVentaId());
    }

    @Test
    void ventaPorLoteCarneadoCalculaPrecioSegunPesoDeCadaAnimal() {
        UUID animalId2 = UUID.randomUUID();
        when(animales.findById(animalId2, empresa)).thenReturn(Optional.of(animal(animalId2, "ANI-000002")));

        List<Venta> ventas = service.registrarLote(new VentaLoteCommand(List.of(animalId, animalId2),
                LocalDate.now(), "Frigorífico Norte", null, ModalidadVenta.CARNEADO,
                null, new BigDecimal("15"), Map.of(animalId, new BigDecimal("380"), animalId2, new BigDecimal("400")),
                null));

        assertThat(ventas).hasSize(2);
        assertThat(ventas.get(0).precio()).isEqualByComparingTo("5700.00");
        assertThat(ventas.get(1).precio()).isEqualByComparingTo("6000.00");
        assertThat(ventas).allSatisfy(v -> assertThat(v.precioUnitario()).isEqualByComparingTo("15"));
    }

    @Test
    void rechazaUnLoteVacio() {
        assertThatThrownBy(() -> service.registrarLote(new VentaLoteCommand(List.of(), LocalDate.now(),
                "Comprador", null, ModalidadVenta.EN_PIE, new BigDecimal("100"), null, Map.of(), null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VENTA_LOTE_VACIO));
    }

    @Test
    void rechazaCarneadoSiFaltaElPesoDeUnAnimal() {
        UUID animalId2 = UUID.randomUUID();
        when(animales.findById(animalId2, empresa)).thenReturn(Optional.of(animal(animalId2, "ANI-000002")));

        assertThatThrownBy(() -> service.registrarLote(new VentaLoteCommand(List.of(animalId, animalId2),
                LocalDate.now(), "Comprador", null, ModalidadVenta.CARNEADO, null, new BigDecimal("15"),
                Map.of(animalId, new BigDecimal("380")), null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VENTA_PESO_REQUERIDO));
    }

    @Test
    void detieneElLoteAlPrimerAnimalConRetiroSanitarioVigente() {
        UUID animalId2 = UUID.randomUUID();
        when(animales.findById(animalId2, empresa)).thenReturn(Optional.of(animal(animalId2, "ANI-000002")));
        @SuppressWarnings("unchecked")
        ObjectProvider<RestriccionRetiroPort> conRetiro = mock(ObjectProvider.class);
        RestriccionRetiroPort port = mock(RestriccionRetiroPort.class);
        when(conRetiro.getIfAvailable()).thenReturn(port);
        when(port.vigente(eq(empresa), eq(animalId2), any())).thenReturn(
                Optional.of(new RestriccionRetiroPort.RestriccionRetiroVigente("CARNE", LocalDate.now().plusDays(5))));
        CurrentUser user = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of(),
                Set.of("VENTA_REGISTRAR", "VENTA_VER"), Set.of(), true);
        VentaService conBloqueo = new VentaService(ventas, animales, movimientos, pesajes,
                new UserContext(() -> user), conRetiro);

        assertThatThrownBy(() -> conBloqueo.registrarLote(new VentaLoteCommand(List.of(animalId, animalId2),
                LocalDate.now(), "Comprador", null, ModalidadVenta.EN_PIE, new BigDecimal("3500"), null, Map.of(), null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VENTA_RETIRO_SANITARIO_VIGENTE));
        verify(ventas, times(1)).create(any());
    }
}
