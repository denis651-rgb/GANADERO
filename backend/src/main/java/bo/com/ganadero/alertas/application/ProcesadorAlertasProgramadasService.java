package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.EntregaRepository;
import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Convierte alertas PROGRAMADA en PENDIENTE cuando vence su fecha y envía las
 * notificaciones Web Push a los dispositivos suscritos de la empresa.
 */
@Service
public class ProcesadorAlertasProgramadasService {
    private final AlertaRepository alertas;
    private final SuscripcionPushRepository suscripciones;
    private final EntregaRepository entregas;
    private final ObjectProvider<PushNotificadorPort> notificador;
    private final boolean pushHabilitado;
    private final int maxIntentos;

    public ProcesadorAlertasProgramadasService(AlertaRepository alertas, SuscripcionPushRepository suscripciones,
                                               EntregaRepository entregas, ObjectProvider<PushNotificadorPort> notificador,
                                               @Value("${ganadero.push.enabled:false}") boolean pushHabilitado,
                                               @Value("${ganadero.push.max-attempts:5}") int maxIntentos) {
        this.alertas = alertas;
        this.suscripciones = suscripciones;
        this.entregas = entregas;
        this.notificador = notificador;
        this.pushHabilitado = pushHabilitado;
        this.maxIntentos = Math.max(1, maxIntentos);
    }

    @Scheduled(cron = "${ganadero.alertas.cron-activar:0 */5 * * * *}")
    @Transactional
    public int activarVencidas() {
        return alertas.activarVencidas(Instant.now(), 200);
    }

    @Scheduled(cron = "${ganadero.alertas.cron-enviar:0 */5 * * * *}")
    @Transactional
    public int enviarPendientes() {
        PushNotificadorPort push = notificador.getIfAvailable();
        if (!pushHabilitado || push == null) {
            return 0;
        }
        List<Alerta> pendientes = alertas.listarPendientesEnvio(Instant.now(), maxIntentos, 50);
        int enviadas = 0;
        for (Alerta alerta : pendientes) {
            List<SuscripcionPush> subs = suscripciones.listarActivas(alerta.empresaId());
            boolean algunaEnviada = false;
            boolean algunaInvalida = false;
            int elegibles = 0;
            StringBuilder errores = new StringBuilder();
            for (SuscripcionPush sub : subs) {
                if (!deseaRecibir(alerta, sub)) {
                    continue;
                }
                elegibles++;
                entregas.registrarPendiente(alerta.id(), sub.id());
                PushNotificadorPort.ResultadoEnvio resultado;
                try {
                    resultado = push.enviar(alerta, sub);
                } catch (Exception ex) {
                    resultado = PushNotificadorPort.ResultadoEnvio.fallo(
                            ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                }
                if (resultado.exitoso()) {
                    entregas.marcarEnviada(alerta.id(), sub.id(), Instant.now());
                    algunaEnviada = true;
                } else {
                    entregas.marcarError(alerta.id(), sub.id(), resultado.error());
                    if (resultado.suscripcionInvalida()) {
                        suscripciones.desactivarTodas(sub.id(), alerta.empresaId());
                        algunaInvalida = true;
                    } else if (errores.length() < 500) {
                        if (errores.length() > 0) {
                            errores.append("; ");
                        }
                        errores.append(resultado.error());
                    }
                }
            }
            if (algunaEnviada) {
                alertas.marcarEnviada(alerta.id());
                enviadas++;
            } else if (elegibles == 0 || algunaInvalida) {
                alertas.marcarEnviada(alerta.id());
            } else {
                alertas.registrarFallo(alerta.id(), errores.toString(), maxIntentos);
            }
        }
        return enviadas;
    }

    private boolean deseaRecibir(Alerta alerta, SuscripcionPush sub) {
        PreferenciasNotificacion prefs = suscripciones.preferencias(alerta.empresaId(), sub.usuarioId());
        if (!porSeveridad(prefs, alerta.severidad())) {
            return false;
        }
        return switch (alerta.tipo()) {
            case PROXIMO_PARTO, DIAGNOSTICO_GESTACION_PENDIENTE, DESTETE_PROXIMO -> prefs.reproduccion();
            case TRATAMIENTO_PENDIENTE, TRATAMIENTO_ATRASADO -> prefs.tratamientos();
            case CASO_CLINICO_CRITICO -> prefs.casosCriticos();
            case VACUNACION_PROXIMA, RETIRO_SANITARIO, CUARENTENA_POR_FINALIZAR -> prefs.sanidad();
        };
    }

    private boolean porSeveridad(PreferenciasNotificacion prefs, SeveridadAlerta severidad) {
        return switch (severidad) {
            case CRITICA -> prefs.criticas();
            case URGENTE -> prefs.urgentes();
            case INFO, WARNING -> prefs.recordatorios();
        };
    }
}
