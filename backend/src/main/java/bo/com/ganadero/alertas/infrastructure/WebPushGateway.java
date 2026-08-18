package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.PushNotificadorPort;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.security.Security;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class WebPushGateway implements PushNotificadorPort {
    private static final Pattern BASE64_URL = Pattern.compile("^[A-Za-z0-9_-]+={0,2}$");
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final boolean enabled;
    private final int ttlSeconds;
    private final String frontendUrl;
    private final ObjectMapper json;
    private volatile PushService service;

    @Autowired
    public WebPushGateway(@Value("${ganadero.push.vapid-public-key:}") String publicKey,
                          @Value("${ganadero.push.vapid-private-key:}") String privateKey,
                          @Value("${ganadero.push.subject:mailto:soporte@ganadero.bo}") String subject,
                          @Value("${ganadero.push.enabled:false}") boolean enabled,
                          @Value("${ganadero.push.ttl-seconds:604800}") int ttlSeconds,
                          @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                          ObjectMapper json) {
        this.publicKey = clean(publicKey);
        this.privateKey = clean(privateKey);
        this.subject = clean(subject);
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
        this.frontendUrl = frontendUrl;
        this.json = json;
        Security.addProvider(new BouncyCastleProvider());
    }

    WebPushGateway(String publicKey, String privateKey, String subject, int ttlSeconds,
                   String frontendUrl, ObjectMapper json, PushService service) {
        this.publicKey = clean(publicKey);
        this.privateKey = clean(privateKey);
        this.subject = clean(subject);
        this.enabled = true;
        this.ttlSeconds = ttlSeconds;
        this.frontendUrl = frontendUrl;
        this.json = json;
        this.service = service;
        Security.addProvider(new BouncyCastleProvider());
    }

    @PostConstruct
    void validarConfiguracion() {
        if (!enabled) return;
        if (!configuracionCompleta()) {
            throw new IllegalStateException("Web Push habilitado pero configuración VAPID incompleta");
        }
        try {
            validarFormato();
            servicio();
        } catch (Exception ex) {
            throw new IllegalStateException("Web Push habilitado pero la configuración VAPID es inválida", ex);
        }
    }

    @Override
    public ResultadoEnvio enviar(Alerta alerta, SuscripcionPush suscripcion) {
        PushService current = servicio();
        if (current == null) {
            return ResultadoEnvio.configuracion("WEB_PUSH_CONFIG_ERROR: VAPID no configurado");
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
            HttpResponse response = current.send(notification, Encoding.AES128GCM);
            status = response.getStatusLine() == null ? 0 : response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() == null ? null : EntityUtils.toString(response.getEntity());
            if (status >= 200 && status < 300) {
                return ResultadoEnvio.ok();
            }
            return ResultadoEnvio.http(status, errorHttp(status, responseBody));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResultadoEnvio.fallo("WEB_PUSH_INTERRUPTED: envío interrumpido");
        } catch (Exception ex) {
            return status > 0
                    ? ResultadoEnvio.http(status, errorHttp(status, mensaje(ex)))
                    : ResultadoEnvio.fallo("WEB_PUSH_TRANSPORT_ERROR: " + sanitizar(mensaje(ex), 500));
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
                if (!configuracionCompleta()) {
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

    private boolean configuracionCompleta() {
        return !publicKey.isBlank() && !privateKey.isBlank() && !subject.isBlank();
    }

    private void validarFormato() {
        if (!BASE64_URL.matcher(publicKey).matches() || !BASE64_URL.matcher(privateKey).matches()) {
            throw new IllegalArgumentException("Las claves VAPID deben usar Base64 URL-safe");
        }
        decodeUrl(publicKey);
        decodeUrl(privateKey);
        URI uri = URI.create(subject);
        if (!("mailto".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("GANADERO_PUSH_SUBJECT debe ser mailto: o https:");
        }
    }

    private String mensaje(Exception ex) {
        String text = ex.getMessage();
        return text == null || text.isBlank() ? ex.getClass().getSimpleName() : text;
    }

    private String errorHttp(int status, String body) {
        String codigo = "WEB_PUSH_HTTP_" + status;
        String detalle = body == null || body.isBlank() ? descripcion(status) : sanitizar(body, 500);
        return codigo + " [" + Instant.now() + "]: " + detalle;
    }

    private String descripcion(int status) {
        return switch (status) {
            case 400 -> "petición Web Push inválida";
            case 401 -> "autorización Web Push inválida";
            case 403 -> "VAPID, encabezado o protocolo rechazado";
            case 404 -> "endpoint Push inexistente";
            case 410 -> "suscripción Push expirada";
            case 429 -> "límite temporal del proveedor Push";
            default -> status >= 500 ? "error temporal del proveedor Push" : "error del proveedor Push";
        };
    }

    private String sanitizar(String value, int max) {
        String limpio = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        return limpio.substring(0, Math.min(limpio.length(), max));
    }

    private static String clean(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static void decodeUrl(String value) {
        String unpadded = value.replaceAll("=+$", "");
        Base64.getUrlDecoder().decode(unpadded + "=".repeat((4 - unpadded.length() % 4) % 4));
    }
}
