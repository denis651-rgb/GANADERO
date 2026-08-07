package bo.com.ganadero.dashboard.application;

import bo.com.ganadero.dashboard.domain.DashboardRepository;
import bo.com.ganadero.dashboard.domain.DashboardResumen;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    private final DashboardRepository repository = mock(DashboardRepository.class);

    private DashboardService service(CurrentUser user) {
        return new DashboardService(repository, new UserContext(() -> user));
    }

    private CurrentUser propietario(UUID empresa) {
        return new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of("PROPIETARIO"),
                Set.of("DASHBOARD_VER"), Set.of(), true);
    }

    private CurrentUser trabajador(UUID empresa, UUID propiedad) {
        return new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of("TRABAJADOR"),
                Set.of("DASHBOARD_VER"), Set.of(propiedad), false);
    }

    @Test
    void propietarioVeIndicadoresDeTodaLaEmpresa() {
        UUID empresa = UUID.randomUUID();
        when(repository.countAnimales(empresa, true, Set.of())).thenReturn(120L);
        when(repository.animalesPorCategoria(empresa, true, Set.of()))
                .thenReturn(List.of(new DashboardResumen.Distribucion("Vaca", 60)));

        DashboardResumen resumen = service(propietario(empresa)).resumen();

        assertThat(resumen.totalAnimales()).isEqualTo(120L);
        verify(repository).countAnimales(empresa, true, Set.of());
        verify(repository).countPesajesUltimosDias(empresa, 7, true, Set.of());
        verify(repository).countMovimientosUltimosDias(empresa, 7, true, Set.of());
    }

    @Test
    void trabajadorVeSoloIndicadoresDePropiedadAsignada() {
        UUID empresa = UUID.randomUUID();
        UUID propiedad = UUID.randomUUID();
        when(repository.countAnimales(empresa, false, Set.of(propiedad))).thenReturn(30L);

        DashboardResumen resumen = service(trabajador(empresa, propiedad)).resumen();

        assertThat(resumen.totalAnimales()).isEqualTo(30L);
        verify(repository).countAnimales(empresa, false, Set.of(propiedad));
        verify(repository).countPesajesUltimosDias(empresa, 7, false, Set.of(propiedad));
        verify(repository).countMovimientosUltimosDias(empresa, 7, false, Set.of(propiedad));
        verify(repository, never()).countAnimales(empresa, true, Set.of());
    }

    @Test
    void usuarioSinPermisoNoAccedeAlResumen() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        assertThatThrownBy(() -> service(user).resumen())
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.USER_NOT_AUTHORIZED));
        verifyNoInteractions(repository);
    }

    @Test
    void consultaSeFiltraPorEmpresaDelUsuario() {
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        DashboardService service = service(propietario(empresaA));

        service.resumen();

        verify(repository).countAnimales(empresaA, true, Set.of());
        verify(repository, never()).countAnimales(empresaB, true, Set.of());
        verify(repository).pesajesRecientes(empresaA, true, Set.of(), 8);
    }

    @Test
    void datosCorrectosSeExponenEnElResumen() {
        UUID empresa = UUID.randomUUID();
        when(repository.countAnimales(empresa, true, Set.of())).thenReturn(120L);
        when(repository.countAnimalesEnPotrero(empresa, true, Set.of())).thenReturn(95L);
        when(repository.countLotesActivos(empresa, true, Set.of())).thenReturn(6L);
        when(repository.countPotrerosActivos(empresa, true, Set.of())).thenReturn(14L);
        when(repository.pesoPromedio(empresa, true, Set.of())).thenReturn(380.5);
        when(repository.gananciaDiaria(empresa, true, Set.of())).thenReturn(0.42);
        when(repository.countPesajesUltimosDias(empresa, 7, true, Set.of())).thenReturn(33L);
        when(repository.countMovimientosUltimosDias(empresa, 7, true, Set.of())).thenReturn(5L);
        when(repository.countAnimalesSinPesaje(empresa, true, Set.of())).thenReturn(7L);
        when(repository.countAnimalesGananciaNegativa(empresa, true, Set.of())).thenReturn(2L);
        when(repository.countPotrerosInactivos(empresa, true, Set.of())).thenReturn(1L);
        when(repository.countLotesCerrados(empresa, true, Set.of())).thenReturn(0L);
        when(repository.animalesPorCategoria(empresa, true, Set.of()))
                .thenReturn(List.of(new DashboardResumen.Distribucion("Vaca", 60)));
        when(repository.animalesPorPotrero(empresa, true, Set.of()))
                .thenReturn(List.of(new DashboardResumen.Distribucion("Potrero Norte", 40)));
        when(repository.animalesPorLote(empresa, true, Set.of()))
                .thenReturn(List.of(new DashboardResumen.Distribucion("Lote A", 25)));
        when(repository.pesajesRecientes(empresa, true, Set.of(), 8))
                .thenReturn(List.of(new DashboardResumen.PesajeReciente(
                        UUID.randomUUID(), UUID.randomUUID(), "A-100", "Vaca 100",
                        LocalDate.of(2026, 8, 6), new BigDecimal("385.500"))));

        DashboardResumen resumen = service(propietario(empresa)).resumen();

        assertThat(resumen.totalAnimales()).isEqualTo(120L);
        assertThat(resumen.animalesEnPotrero()).isEqualTo(95L);
        assertThat(resumen.pesoPromedioKg()).isEqualTo(380.5);
        assertThat(resumen.gananciaPromedioKg()).isEqualTo(0.42);
        assertThat(resumen.animalesPorCategoria()).hasSize(1);
        assertThat(resumen.pesajesRecientes()).hasSize(1);
        assertThat(resumen.alertas()).extracting(DashboardResumen.AlertaBasica::tipo)
                .containsExactly("SIN_PESAJE", "GANANCIA_NEGATIVA", "POTREROS_INACTIVOS");
    }

    @Test
    void sinRegistrosDevuelveCerosYListasVacias() {
        UUID empresa = UUID.randomUUID();
        when(repository.pesoPromedio(empresa, true, Set.of())).thenReturn(null);
        when(repository.gananciaDiaria(empresa, true, Set.of())).thenReturn(null);

        DashboardResumen resumen = service(propietario(empresa)).resumen();

        assertThat(resumen.totalAnimales()).isZero();
        assertThat(resumen.pesoPromedioKg()).isNull();
        assertThat(resumen.gananciaPromedioKg()).isNull();
        assertThat(resumen.animalesPorCategoria()).isEmpty();
        assertThat(resumen.pesajesRecientes()).isEmpty();
        assertThat(resumen.alertas()).isEmpty();
    }
}
