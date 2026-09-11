package bo.com.ganadero.reportes.application;

import bo.com.ganadero.reportes.domain.ReporteRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteServiceTest {
    private ReporteRepository reportes;
    private ReporteService service;

    @BeforeEach
    void setUp() {
        reportes = mock(ReporteRepository.class);
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                Set.of("REPORTE_VER"), Set.of(), true);
        service = new ReporteService(reportes, new UserContext(() -> user));
    }

    @Test
    void rechazaUnRangoDondeDesdeEsPosteriorAHasta() {
        assertThatThrownBy(() -> service.nacimientos(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-01-01")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void delegaLosNacimientosAlRepositorio() {
        LocalDate desde = LocalDate.parse("2026-01-01");
        LocalDate hasta = LocalDate.parse("2026-03-31");
        when(reportes.nacidos(desde, hasta)).thenReturn(List.of());

        assertThat(service.nacimientos(desde, hasta)).isEmpty();
        verify(reportes).nacidos(desde, hasta);
    }

    @Test
    void delegaLasMuertesAlRepositorio() {
        LocalDate desde = LocalDate.parse("2026-01-01");
        LocalDate hasta = LocalDate.parse("2026-03-31");
        when(reportes.muertos(any(), any())).thenReturn(List.of());

        service.muertes(desde, hasta);

        verify(reportes).muertos(desde, hasta);
    }

    @Test
    void delegaLasVentasAlRepositorio() {
        LocalDate desde = LocalDate.parse("2026-01-01");
        LocalDate hasta = LocalDate.parse("2026-03-31");
        when(reportes.ventas(any(), any())).thenReturn(List.of());

        service.ventas(desde, hasta);

        verify(reportes).ventas(desde, hasta);
    }
}
