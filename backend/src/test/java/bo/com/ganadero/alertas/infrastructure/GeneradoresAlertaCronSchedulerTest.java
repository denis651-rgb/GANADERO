package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.RecordatorioService;
import bo.com.ganadero.sanidad.application.CalendarioSanitarioService;
import bo.com.ganadero.pesajes.application.ProcesarPesajesAtrasadosService;
import bo.com.ganadero.sanidad.application.ProcesarTratamientosVencidosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GeneradoresAlertaCronSchedulerTest {
    private CalendarioSanitarioService calendario;
    private GeneradoresAlertaCronScheduler scheduler;

    @BeforeEach
    void setUp() {
        calendario = mock(CalendarioSanitarioService.class);
        scheduler = new GeneradoresAlertaCronScheduler(mock(ProcesarTratamientosVencidosService.class),
                mock(ProcesarPesajesAtrasadosService.class), mock(RecordatorioService.class), calendario);
    }

    @Test
    void alIniciarLaAppGeneraElCalendarioSinEsperarALaCorridaDeLas0010() {
        // Es una app de escritorio: casi nunca está abierta a las 00:10, así que sin esto el calendario
        // de un plan creado con la app cerrada no se generaba hasta cruzar una medianoche con la app abierta.
        scheduler.generarCalendarioAlIniciar();

        verify(calendario).procesar();
    }

    @Test
    void siLaGeneracionFallaAlIniciarLaAppSigueArrancando() {
        doThrow(new IllegalStateException("base de datos ocupada")).when(calendario).procesar();

        assertThatCode(scheduler::generarCalendarioAlIniciar).doesNotThrowAnyException();
    }
}
