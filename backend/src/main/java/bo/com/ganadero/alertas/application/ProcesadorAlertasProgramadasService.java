package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.AlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * App de escritorio: las notificaciones ya no se entregan por Web Push, el
 * proceso de Electron consulta las alertas PENDIENTE directamente y muestra
 * una notificacion nativa del sistema operativo. Este servicio solo activa
 * las alertas PROGRAMADA cuya fecha ya vencio.
 */
@Service
public class ProcesadorAlertasProgramadasService {
    private final AlertaRepository alertas;

    public ProcesadorAlertasProgramadasService(AlertaRepository alertas) {
        this.alertas = alertas;
    }

    @Transactional
    public int activarVencidas() {
        return alertas.activarVencidas(Instant.now(), 200);
    }
}
