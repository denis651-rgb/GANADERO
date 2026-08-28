package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.ProcesadorAlertasProgramadasService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reloj local del motor de alertas para la app de escritorio.
 */
@Component
@ConditionalOnProperty(prefix = "ganadero.alertas.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlertaCronScheduler {
    private final ProcesadorAlertasProgramadasService procesador;

    public AlertaCronScheduler(ProcesadorAlertasProgramadasService procesador) {
        this.procesador = procesador;
    }

    @Scheduled(cron = "${ganadero.alertas.cron-activar:0 */5 * * * *}")
    @Transactional
    public int activarVencidas() {
        return procesador.activarVencidas();
    }
}
