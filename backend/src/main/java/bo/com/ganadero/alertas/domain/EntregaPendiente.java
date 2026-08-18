package bo.com.ganadero.alertas.domain;

import java.util.UUID;

/**
 * Una ENTREGA pendiente de envío, junto con la ALERTA que comunica y la
 * SUSCRIPCIÓN (dispositivo) de destino. Permite procesar cada intento de
 * entrega de forma independiente: una alerta puede tener varias entregas.
 */
public record EntregaPendiente(UUID id, Alerta alerta, SuscripcionPush suscripcion, int intentos) {
}
