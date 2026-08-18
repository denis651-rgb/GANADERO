package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.EntregaPendiente;
import bo.com.ganadero.alertas.domain.EntregaRepository;
import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Procesa las notificaciones Web Push separando ALERTA de ENTREGA.
 * <p>
 * Una ALERTA representa un acontecimiento que merece atención (ej. tratamiento
 * atrasado). Una ENTREGA es el intento de comunicar esa alerta a un dispositivo;
 * por lo tanto una alerta puede tener varias entregas, una por suscripción.
 * <p>
 * El flujo no crea ni oculta alertas del centro: para las alertas PENDIENTE
 * materializa únicamente las entregas Push permitidas por las preferencias del
 * usuario y luego envía cada entrega pendiente.
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

    /** Convierte alertas PROGRAMADA en PENDIENTE cuando vence su fecha. */
    @Transactional
    public int activarVencidas() {
        return alertas.activarVencidas(Instant.now(), 200);
    }

    /** Materializa y envía las notificaciones pendientes. No crea alertas nuevas. */
    @Transactional
    public int procesarNotificacionesPendientes() {
        PushNotificadorPort push = notificador.getIfAvailable();
        if (!pushHabilitado || push == null) {
            return 0;
        }
        materializarEntregas();
        return enviarEntregasPendientes(push);
    }

    /** Para cada ALERTA pendiente crea las ENTREGAS de los dispositivos elegibles. */
    private void materializarEntregas() {
        List<Alerta> pendientes = alertas.listarPendientes(Instant.now(), 50);
        for (Alerta alerta : pendientes) {
            for (SuscripcionPush sub : suscripciones.listarActivas(alerta.empresaId())) {
                if (deseaRecibir(alerta, sub)) {
                    entregas.registrarPendiente(alerta.id(), sub.id());
                }
            }
            if (!entregas.tienePendientes(alerta.id(), maxIntentos)) {
                alertas.marcarEnviada(alerta.id());
            }
        }
    }

    /** Envía las ENTREGAS pendientes y cierra la alerta cuando no quedan entregas. */
    private int enviarEntregasPendientes(PushNotificadorPort push) {
        List<EntregaPendiente> pendientes = entregas.listarPendientes(maxIntentos, 50);
        Set<UUID> alertasTocadas = new LinkedHashSet<>();
        int enviadas = 0;
        for (EntregaPendiente entrega : pendientes) {
            alertasTocadas.add(entrega.alerta().id());
            PushNotificadorPort.ResultadoEnvio resultado;
            try {
                resultado = push.enviar(entrega.alerta(), entrega.suscripcion());
            } catch (Exception ex) {
                resultado = PushNotificadorPort.ResultadoEnvio.fallo(
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            }
            if (resultado.exitoso()) {
                entregas.marcarEnviada(entrega.alerta().id(), entrega.suscripcion().id(), Instant.now());
                enviadas++;
            } else {
                entregas.marcarError(entrega.alerta().id(), entrega.suscripcion().id(), resultado.error());
                if (resultado.suscripcionInvalida()) {
                    suscripciones.desactivarTodas(entrega.suscripcion().id(), entrega.alerta().empresaId());
                    entregas.marcarDescartada(entrega.alerta().id(), entrega.suscripcion().id());
                }
            }
        }
        for (UUID alertaId : alertasTocadas) {
            if (!entregas.tienePendientes(alertaId, maxIntentos)) {
                alertas.marcarEnviada(alertaId);
            }
        }
        return enviadas;
    }

    private boolean deseaRecibir(Alerta alerta, SuscripcionPush sub) {
        PreferenciasNotificacion prefs = suscripciones.preferencias(alerta.empresaId(), sub.usuarioId());
        return prefs.permite(alerta.tipo()) && porSeveridad(prefs, alerta.severidad());
    }

    private boolean porSeveridad(PreferenciasNotificacion prefs, SeveridadAlerta severidad) {
        return switch (severidad) {
            case CRITICA -> prefs.criticas();
            case URGENTE -> prefs.urgentes();
            case INFO, WARNING -> prefs.recordatorios();
        };
    }
}
