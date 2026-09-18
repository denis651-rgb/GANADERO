package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.sanidad.domain.EstadoEventoCalendario;
import bo.com.ganadero.sanidad.domain.EventoCalendarioSanitario;
import bo.com.ganadero.sanidad.domain.ModalidadActividad;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Qué evento del calendario cierra una aplicación (sección: aplicar tarde también cumple la actividad). */
class EventoQueCierraLaAplicacionTest {
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final LocalDate APLICACION = LocalDate.of(2026, 10, 12);

    @Test
    void aplicarUnosDiasTardeCierraLaFechaVencidaEnVezDeLaDelCicloSiguiente() {
        EventoCalendarioSanitario vencido = evento(EstadoEventoCalendario.VENCIDO, APLICACION.minusDays(3));
        EventoCalendarioSanitario siguiente = evento(EstadoEventoCalendario.PROGRAMADO, APLICACION.plusDays(18));

        assertThat(ReglasSanitarias.eventoQueCierraLaAplicacion(List.of(vencido, siguiente), APLICACION))
                .contains(vencido);
    }

    @Test
    void aplicarUnosDiasAntesCierraLaProximaEnVezDeLaVencidaDelCicloAnterior() {
        EventoCalendarioSanitario anterior = evento(EstadoEventoCalendario.VENCIDO, APLICACION.minusDays(18));
        EventoCalendarioSanitario proxima = evento(EstadoEventoCalendario.PROGRAMADO, APLICACION.plusDays(3));

        assertThat(ReglasSanitarias.eventoQueCierraLaAplicacion(List.of(anterior, proxima), APLICACION))
                .contains(proxima);
    }

    @Test
    void unVencidoMuyAntiguoNoLeGanaAUnaFechaCercanaYQuedaComoIncumplimientoPasado() {
        EventoCalendarioSanitario antiguo = evento(EstadoEventoCalendario.VENCIDO, APLICACION.minusDays(105));
        EventoCalendarioSanitario cercana = evento(EstadoEventoCalendario.PROYECTADO, APLICACION.plusDays(18));

        assertThat(ReglasSanitarias.eventoQueCierraLaAplicacion(List.of(antiguo, cercana), APLICACION))
                .contains(cercana);
    }

    @Test
    void unaActividadSinMasFechasQueUnaVencidaLaCierraSinImportarCuantoSeAtraso() {
        // Por edad: hay una sola fecha por animal; aplicarla meses después sigue cumpliéndola.
        EventoCalendarioSanitario unica = evento(EstadoEventoCalendario.VENCIDO, APLICACION.minusDays(200));

        assertThat(ReglasSanitarias.eventoQueCierraLaAplicacion(List.of(unica), APLICACION)).contains(unica);
    }

    @Test
    void aIgualDistanciaCierraElMasAntiguo() {
        EventoCalendarioSanitario antes = evento(EstadoEventoCalendario.VENCIDO, APLICACION.minusDays(5));
        EventoCalendarioSanitario despues = evento(EstadoEventoCalendario.PROGRAMADO, APLICACION.plusDays(5));

        assertThat(ReglasSanitarias.eventoQueCierraLaAplicacion(List.of(despues, antes), APLICACION)).contains(antes);
    }

    @Test
    void sinEventosNoCierraNada() {
        assertThat(ReglasSanitarias.eventoQueCierraLaAplicacion(List.of(), APLICACION)).isEmpty();
    }

    private EventoCalendarioSanitario evento(EstadoEventoCalendario estado, LocalDate fecha) {
        Instant prevista = fecha.atTime(LocalTime.of(8, 0)).atZone(ZONA).toInstant();
        return new EventoCalendarioSanitario(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                "PERIODO:" + fecha, prevista, prevista, prevista, estado, ModalidadActividad.PERIODICA, null, null,
                null, UUID.randomUUID(), "NORMAL", Instant.now(), 0);
    }
}
