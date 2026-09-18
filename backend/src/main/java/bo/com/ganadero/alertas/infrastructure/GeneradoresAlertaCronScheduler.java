package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.pesajes.application.ProcesarPesajesAtrasadosService;
import bo.com.ganadero.sanidad.application.CalendarioSanitarioService;
import bo.com.ganadero.sanidad.application.ProcesarTratamientosVencidosService;
import bo.com.ganadero.alertas.application.RecordatorioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reloj local; en producción estos generadores los dispara Supabase Cron.
 *
 * <p>Corte controlado (sección 19-20 de la reescritura de sanidad genérica): antes había dos
 * generadores separados sólo para VACUNACION ({@code ProcesarAlertasVacunacionService} y
 * {@code ProyectarCalendarioSanitarioService}); ambos quedan sin programar aquí (el código no se
 * borra, sigue compilando y con sus tests, por si hace falta volver atrás) porque
 * {@link CalendarioSanitarioService} ya cubre VACUNACION igual que antes, además de cualquier
 * otro tipo de actividad. Reactivar los viejos junto con el nuevo duplicaría alertas para el
 * mismo evento real.</p>
 */
@Component
@ConditionalOnProperty(prefix = "ganadero.alertas.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class GeneradoresAlertaCronScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(GeneradoresAlertaCronScheduler.class);
    private final ProcesarTratamientosVencidosService tratamientos;
    private final ProcesarPesajesAtrasadosService pesajes;
    private final RecordatorioService recordatorios;
    private final CalendarioSanitarioService calendario;

    public GeneradoresAlertaCronScheduler(ProcesarTratamientosVencidosService tratamientos,
                                          ProcesarPesajesAtrasadosService pesajes, RecordatorioService recordatorios,
                                          CalendarioSanitarioService calendario) {
        this.tratamientos = tratamientos;
        this.pesajes = pesajes;
        this.recordatorios = recordatorios;
        this.calendario = calendario;
    }

    /**
     * Ganadero es una app de escritorio y casi nunca está abierta a las 00:10: sin esto, el calendario
     * de un plan creado o de un día en que la app estuvo cerrada no se generaba hasta la próxima noche.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void generarCalendarioAlIniciar() {
        try {
            calendario.procesar();
        } catch (RuntimeException e) {
            LOG.warn("No se pudo generar el calendario sanitario al iniciar; se reintentará en la corrida nocturna.", e);
        }
    }

    @Scheduled(cron = "${ganadero.sanidad.cron-calendario-proyectado:0 10 0 * * *}")
    public int generarCalendarioProyectado() { return calendario.procesar(); }

    @Scheduled(cron = "${ganadero.sanidad.cron-tratamientos-vencidos:0 */15 * * * *}")
    public int generarTratamientosVencidos() { return tratamientos.procesar(); }

    @Scheduled(cron = "${ganadero.pesajes.cron-alertas-atrasadas:0 15 0 * * *}")
    public int generarPesajesAtrasados() { return pesajes.procesar(); }

    @Scheduled(cron = "${ganadero.alertas.cron-recordatorios:0 */1 * * * *}")
    public int procesarRecordatorios() { return recordatorios.procesar(); }
}
