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
            boolean elegible = false;
            for (SuscripcionPush sub : suscripciones.listarActivas(alerta.empresaId())) {
                if (deseaRecibir(alerta, sub)) {
                    elegible = true;
                    entregas.registrarPendiente(alerta.id(), sub.id());
                }
            }
            if (!elegible && !entregas.tienePendientes(alerta.id(), maxIntentos)
                    && !entregas.tieneEnviadas(alerta.id())) {
                alertas.marcarError(alerta.id(), "No hay dispositivos Push elegibles para esta alerta");
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
                entregas.marcarError(entrega.alerta().id(), entrega.suscripcion().id(), resultado.error(),
                        proximoIntento(entrega.intentos() + 1));
                if (resultado.suscripcionInvalida()) {
                    suscripciones.desactivarTodas(entrega.suscripcion().id(), entrega.alerta().empresaId());
                    entregas.marcarDescartada(entrega.alerta().id(), entrega.suscripcion().id());
                }
            }
        }
        for (UUID alertaId : alertasTocadas) {
            if (!entregas.tienePendientes(alertaId, maxIntentos)) {
                if (entregas.tieneEnviadas(alertaId)) {
                    alertas.marcarEnviada(alertaId);
                } else {
                    alertas.marcarError(alertaId, entregas.resumenFallos(alertaId));
                }
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

    private Instant proximoIntento(int intento) {
        long minutos = switch (intento) {
            case 1 -> 5;
            case 2 -> 15;
            case 3 -> 30;
            case 4 -> 60;
            default -> 120;
        };
        return Instant.now().plusSeconds(minutos * 60);
    }
}
