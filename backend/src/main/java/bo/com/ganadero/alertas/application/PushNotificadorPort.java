package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;

/** Puerto para el envío efectivo de notificaciones Web Push a un dispositivo. */
public interface PushNotificadorPort {
    ResultadoEnvio enviar(Alerta alerta, SuscripcionPush suscripcion);

    record ResultadoEnvio(boolean exitoso, String error, String codigo, boolean suscripcionInvalida,
                          boolean reintentable, Integer httpStatus) {
        public static ResultadoEnvio ok() {
            return new ResultadoEnvio(true, null, null, false, false, null);
        }

        public static ResultadoEnvio fallo(String error) {
            return new ResultadoEnvio(false, error, "WEB_PUSH_ERROR", false, true, null);
        }

        public static ResultadoEnvio configuracion(String error) {
            return new ResultadoEnvio(false, error, "WEB_PUSH_CONFIG_ERROR", false, false, null);
        }

        public static ResultadoEnvio invalida(String error) {
            return new ResultadoEnvio(false, error, "WEB_PUSH_SUBSCRIPTION_INVALID", true, false, null);
        }

        public static ResultadoEnvio http(int status, String error) {
            boolean invalida = status == 404 || status == 410;
            boolean reintentable = status == 429 || status >= 500;
            return new ResultadoEnvio(false, error, "WEB_PUSH_HTTP_" + status, invalida, reintentable, status);
        }
    }
}
