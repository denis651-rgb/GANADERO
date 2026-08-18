package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.ProcesadorAlertasProgramadasService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reloj local del motor de alertas. Solo se registra cuando el scheduler de
 * Spring está habilitado (desarrollo local). En producción debe estar
 * desactivado y el disparo lo hace Supabase Cron vía /api/internal/jobs.
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

    @Scheduled(cron = "${ganadero.alertas.cron-enviar:0 */5 * * * *}")
    @Transactional
    public int enviarPendientes() {
        return procesador.procesarNotificacionesPendientes();
    }
}
