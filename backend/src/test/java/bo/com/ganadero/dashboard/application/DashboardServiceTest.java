package bo.com.ganadero.dashboard.application;

import bo.com.ganadero.alertas.application.AlertaQueryService;
import bo.com.ganadero.alertas.application.AlertaQueryService.PendientesPorTipo;
import bo.com.ganadero.alertas.application.CategoriaAlerta;
import bo.com.ganadero.alertas.application.NivelAtencion;
import bo.com.ganadero.alertas.application.TipoAlerta;
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
    private final AlertaQueryService alertas = mock(AlertaQueryService.class);

    private DashboardService service(CurrentUser user) {
        return new DashboardService(repository, alertas, new UserContext(() -> user));
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
        when(repository.countPesajesUltimosDias(empresa, 7, true, Set.of())).thenReturn(33L);
        when(repository.countMovimientosUltimosDias(empresa, 7, true, Set.of())).thenReturn(5L);
        when(repository.countAnimalesSinPesaje(empresa, true, Set.of())).thenReturn(7L);
        when(repository.countPotrerosInactivos(empresa, true, Set.of())).thenReturn(1L);
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
        assertThat(resumen.animalesSinPesaje()).isEqualTo(7L);
        assertThat(resumen.animalesPorCategoria()).hasSize(1);
        assertThat(resumen.pesajesRecientes()).hasSize(1);
        assertThat(resumen.alertas()).extracting(DashboardResumen.AlertaBasica::tipo)
                .containsExactly("POTREROS_INACTIVOS");
    }

    @Test
    void sinRegistrosDevuelveCerosYListasVacias() {
        UUID empresa = UUID.randomUUID();
        when(repository.pesoPromedio(empresa, true, Set.of())).thenReturn(null);

        DashboardResumen resumen = service(propietario(empresa)).resumen();

        assertThat(resumen.totalAnimales()).isZero();
        assertThat(resumen.pesoPromedioKg()).isNull();
        assertThat(resumen.animalesPorCategoria()).isEmpty();
        assertThat(resumen.pesajesRecientes()).isEmpty();
        assertThat(resumen.alertas()).isEmpty();
    }

    // ---------- «Atención requerida» ----------

    @Test
    void lasAlertasDeSanidadYReproduccionSeAgrupanConSuSeveridadDeLoUrgenteALoInformativo() {
        UUID empresa = UUID.randomUUID();
        when(alertas.pendientesPorTipo()).thenReturn(List.of(
                new PendientesPorTipo(TipoAlerta.CELO_DETECTADO, 1, NivelAtencion.INFORMATIVO),
                new PendientesPorTipo(TipoAlerta.PARTO_PROXIMO, 2, NivelAtencion.ADVERTENCIA),
                new PendientesPorTipo(TipoAlerta.ACTIVIDAD_SANITARIA_VENCIDA, 3, NivelAtencion.URGENTE),
                new PendientesPorTipo(TipoAlerta.VACUNA_VENCIDA, 5, NivelAtencion.URGENTE),
                new PendientesPorTipo(TipoAlerta.CASO_CLINICO_CRITICO, 1, NivelAtencion.URGENTE)));

        var lista = service(propietario(empresa)).resumen().alertas();

        // más urgente primero; a igual nivel, el grupo con más alertas (y, si empatan, por nombre)
        assertThat(lista).extracting(DashboardResumen.AlertaBasica::tipo).containsExactly(
                "VACUNA_VENCIDA", "ACTIVIDAD_SANITARIA_VENCIDA", "CASO_CLINICO_CRITICO", "PARTO_PROXIMO", "CELO_DETECTADO");
        assertThat(lista).extracting(DashboardResumen.AlertaBasica::severidad)
                .containsExactly("danger", "danger", "danger", "warning", "info");
        assertThat(lista.get(0).mensaje()).isEqualTo("Vacunas vencidas");
        assertThat(lista.get(0).total()).isEqualTo(5L);
        assertThat(lista.get(3).mensaje()).isEqualTo("Partos próximos");
    }

    @Test
    void elAvisoDePesajeDelModuloDeAlertasNoSeRepiteEnElDashboard() {
        UUID empresa = UUID.randomUUID();
        when(repository.countAnimalesSinPesaje(empresa, true, Set.of())).thenReturn(7L);
        when(alertas.pendientesPorTipo()).thenReturn(List.of(
                new PendientesPorTipo(TipoAlerta.PESAJE_ATRASADO, 9, NivelAtencion.ADVERTENCIA),
                new PendientesPorTipo(TipoAlerta.PARTO_PROXIMO, 1, NivelAtencion.ADVERTENCIA)));

        DashboardResumen resumen = service(propietario(empresa)).resumen();

        assertThat(resumen.alertas()).extracting(DashboardResumen.AlertaBasica::tipo).containsExactly("PARTO_PROXIMO");
        // el dato sigue disponible para que la pantalla arme su único aviso de pesaje, con botón
        assertThat(resumen.animalesSinPesaje()).isEqualTo(7L);
    }

    @Test
    void yaNoSeAvisaPorSinPesajeNiPorGananciaNegativaDesdeElBackend() {
        UUID empresa = UUID.randomUUID();
        when(repository.countAnimalesSinPesaje(empresa, true, Set.of())).thenReturn(12L);
        when(repository.countPotrerosInactivos(empresa, true, Set.of())).thenReturn(2L);

        var tipos = service(propietario(empresa)).resumen().alertas().stream()
                .map(DashboardResumen.AlertaBasica::tipo).toList();

        assertThat(tipos).containsExactly("POTREROS_INACTIVOS").doesNotContain("SIN_PESAJE", "GANANCIA_NEGATIVA");
    }

    @Test
    void losPotrerosInactivosSeMuestranDespuesDeLasAlertasDeLosModulos() {
        UUID empresa = UUID.randomUUID();
        when(repository.countPotrerosInactivos(empresa, true, Set.of())).thenReturn(1L);
        when(alertas.pendientesPorTipo()).thenReturn(List.of(
                new PendientesPorTipo(TipoAlerta.TRATAMIENTO_ATRASADO, 2, NivelAtencion.URGENTE)));

        var tipos = service(propietario(empresa)).resumen().alertas().stream()
                .map(DashboardResumen.AlertaBasica::tipo).toList();

        assertThat(tipos).containsExactly("TRATAMIENTO_ATRASADO", "POTREROS_INACTIVOS");
    }

    @Test
    void todosLosTiposDeAlertaQueSeMuestranTienenTextoPropio() {
        for (TipoAlerta tipo : TipoAlerta.values()) {
            if (tipo.categoria() == CategoriaAlerta.PESAJE) continue;
            assertThat(DashboardService.etiqueta(tipo)).as("texto de " + tipo).isNotEqualTo(tipo.name());
        }
    }

    @Test
    void elNivelDeAtencionSeTraduceALosTresColoresDelDashboard() {
        assertThat(DashboardService.severidad(NivelAtencion.URGENTE)).isEqualTo("danger");
        assertThat(DashboardService.severidad(NivelAtencion.ADVERTENCIA)).isEqualTo("warning");
        assertThat(DashboardService.severidad(NivelAtencion.INFORMATIVO)).isEqualTo("info");
    }
}
