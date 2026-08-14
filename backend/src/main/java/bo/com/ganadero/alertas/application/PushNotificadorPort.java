package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;

/** Puerto para el envío efectivo de notificaciones Web Push a un dispositivo. */
public interface PushNotificadorPort {
    ResultadoEnvio enviar(Alerta alerta, SuscripcionPush suscripcion);

    record ResultadoEnvio(boolean exitoso, String error, boolean suscripcionInvalida) {
        public static ResultadoEnvio ok() {
            return new ResultadoEnvio(true, null, false);
        }

        public static ResultadoEnvio fallo(String error) {
            return new ResultadoEnvio(false, error, false);
        }

        public static ResultadoEnvio invalida(String error) {
            return new ResultadoEnvio(false, error, true);
        }
    }
}
