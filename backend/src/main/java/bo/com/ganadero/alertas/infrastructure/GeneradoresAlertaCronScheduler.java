package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.pesajes.application.ProcesarPesajesAtrasadosService;
import bo.com.ganadero.sanidad.application.ProcesarAlertasVacunacionService;
import bo.com.ganadero.sanidad.application.ProcesarTratamientosVencidosService;
import bo.com.ganadero.alertas.application.RecordatorioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reloj local; en producción estos generadores los dispara Supabase Cron. */
@Component
@ConditionalOnProperty(prefix = "ganadero.alertas.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class GeneradoresAlertaCronScheduler {
    private final ProcesarAlertasVacunacionService vacunacion;
    private final ProcesarTratamientosVencidosService tratamientos;
    private final ProcesarPesajesAtrasadosService pesajes;
    private final RecordatorioService recordatorios;

    public GeneradoresAlertaCronScheduler(ProcesarAlertasVacunacionService vacunacion,
                                          ProcesarTratamientosVencidosService tratamientos,
                                          ProcesarPesajesAtrasadosService pesajes, RecordatorioService recordatorios) {
        this.vacunacion = vacunacion;
        this.tratamientos = tratamientos;
        this.pesajes = pesajes;
        this.recordatorios = recordatorios;
    }

    @Scheduled(cron = "${ganadero.sanidad.cron-alertas-vacunacion:0 5 0 * * *}")
    public int generarVacunacion() { return vacunacion.procesar(); }

    @Scheduled(cron = "${ganadero.sanidad.cron-tratamientos-vencidos:0 */15 * * * *}")
    public int generarTratamientosVencidos() { return tratamientos.procesar(); }

    @Scheduled(cron = "${ganadero.pesajes.cron-alertas-atrasadas:0 15 0 * * *}")
    public int generarPesajesAtrasados() { return pesajes.procesar(); }

    @Scheduled(cron = "${ganadero.alertas.cron-recordatorios:0 */1 * * * *}")
    public int procesarRecordatorios() { return recordatorios.procesar(); }
}
