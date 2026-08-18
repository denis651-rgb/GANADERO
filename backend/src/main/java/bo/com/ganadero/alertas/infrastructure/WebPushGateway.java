package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.PushNotificadorPort;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebPushGateway implements PushNotificadorPort {
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final int ttlSeconds;
    private final String frontendUrl;
    private final ObjectMapper json;
    private volatile PushService service;

    public WebPushGateway(@Value("${ganadero.push.vapid-public-key:}") String publicKey,
                          @Value("${ganadero.push.vapid-private-key:}") String privateKey,
                          @Value("${ganadero.push.subject:mailto:soporte@ganadero.bo}") String subject,
                          @Value("${ganadero.push.ttl-seconds:604800}") int ttlSeconds,
                          @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                          ObjectMapper json) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.ttlSeconds = ttlSeconds;
        this.frontendUrl = frontendUrl;
        this.json = json;
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public ResultadoEnvio enviar(Alerta alerta, SuscripcionPush suscripcion) {
        PushService current = servicio();
        if (current == null) {
            return ResultadoEnvio.fallo("VAPID no configurado");
        }
        int status = 0;
        try {
            String payload = json.writeValueAsString(payload(alerta));
            Notification notification = Notification.builder()
                    .endpoint(suscripcion.endpoint())
                    .userPublicKey(suscripcion.p256dh())
                    .userAuth(suscripcion.auth())
                    .payload(payload)
                    .ttl(ttlSeconds)
                    .urgency(urgencia(alerta.severidad()))
                    .build();
            HttpResponse response = current.send(notification);
            status = response.getStatusLine() == null ? 0 : response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() == null ? null : EntityUtils.toString(response.getEntity());
            if (status >= 200 && status < 300) {
                return ResultadoEnvio.ok();
            }
            if (status == 404 || status == 410) {
                return ResultadoEnvio.invalida(errorHttp(status, responseBody));
            }
            return ResultadoEnvio.fallo(errorHttp(status, responseBody));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResultadoEnvio.fallo("Interrumpido");
        } catch (Exception ex) {
            return ResultadoEnvio.fallo(status > 0 ? "HTTP " + status : mensaje(ex));
        }
    }

    private Map<String, Object> payload(Alerta alerta) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", frontendUrl + "/alertas");
        data.put("alertaId", alerta.id().toString());
        data.put("tipo", alerta.tipo().name());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", alerta.titulo());
        payload.put("body", alerta.mensaje());
        payload.put("icon", "/icons/icon-192.png");
        payload.put("badge", "/icons/icon-192.png");
        payload.put("data", data);
        return payload;
    }

    private Urgency urgencia(SeveridadAlerta severidad) {
        return switch (severidad) {
            case CRITICA, URGENTE -> Urgency.HIGH;
            case WARNING -> Urgency.NORMAL;
            case INFO -> Urgency.LOW;
        };
    }

    private PushService servicio() {
        PushService current = service;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (service == null) {
                if (publicKey == null || publicKey.isBlank()
                        || privateKey == null || privateKey.isBlank()) {
                    return null;
                }
                try {
                    service = new PushService(publicKey, privateKey, subject);
                } catch (Exception ex) {
                    return null;
                }
            }
            return service;
        }
    }

    private String mensaje(Exception ex) {
        String text = ex.getMessage();
        return text == null || text.isBlank() ? ex.getClass().getSimpleName() : text;
    }

    private String errorHttp(int status, String body) {
        if (body == null || body.isBlank()) {
            return "HTTP " + status;
        }
        String limpio = body.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        return "HTTP " + status + ": " + limpio.substring(0, Math.min(limpio.length(), 500));
    }
}
