package bo.com.ganadero.alertas.application;

import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import bo.com.ganadero.pesajes.application.ProcesarPesajesAtrasadosService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalJobServiceTest {
    @Test
    void ejecutaElJobConElSecretoCorrecto() {
        ProcesadorAlertasProgramadasService procesador = mock(ProcesadorAlertasProgramadasService.class);
        when(procesador.activarVencidas()).thenReturn(3);
        InternalJobService service = new InternalJobService(properties(true, "secreto-largo"), procesador,
                mock(ProcesarPesajesAtrasadosService.class));

        assertThat(service.activarAlertasVencidas("secreto-largo")).isEqualTo(3);
        verify(procesador).activarVencidas();
    }

    @Test
    void rechazaUnSecretoIncorrecto() {
        InternalJobService service = new InternalJobService(properties(true, "secreto-largo"),
                mock(ProcesadorAlertasProgramadasService.class), mock(ProcesarPesajesAtrasadosService.class));

        assertThatThrownBy(() -> service.procesarNotificacionesPendientes("incorrecto"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.INTERNAL_JOBS_TOKEN_INVALID));
    }

    @Test
    void generaAlertasDePesajesDesdeElJobSeguro() {
        ProcesarPesajesAtrasadosService pesajes = mock(ProcesarPesajesAtrasadosService.class);
        when(pesajes.procesar()).thenReturn(5);
        InternalJobService service = new InternalJobService(properties(true, "secreto-largo"),
                mock(ProcesadorAlertasProgramadasService.class), pesajes);

        assertThat(service.generarAlertasPesajes("secreto-largo")).isEqualTo(5);
        verify(pesajes).procesar();
    }

    private AppProperties properties(boolean enabled, String secret) {
        return new AppProperties(new AppProperties.Bootstrap(false, ""),
                new AppProperties.InternalJobs(enabled, secret), new AppProperties.SystemStatus(false),
                "http://localhost", new AppProperties.Storage("bucket", 1024,
                Duration.ofMinutes(5), List.of("image/png"), List.of("png")));
    }
}
