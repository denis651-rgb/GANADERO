package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.PushNotificadorPort;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebPushGatewayTest {
    @Test
    void aceptaConfiguracionVapidValidaConEspacios() throws Exception {
        VapidKeys keys = vapidKeys();
        WebPushGateway gateway = new WebPushGateway("  " + keys.publicKey() + "  ", "  " + keys.privateKey() + "  ",
                "  mailto:soporte@ganadero.bo  ", true, 60, "https://ganadero.app", new ObjectMapper());

        assertThatCode(gateway::validarConfiguracion).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201})
    void enviaPayloadUsandoAes128gcmYAceptaRespuestasExitosas(int status) throws Exception {
        PushService service = mock(PushService.class);
        when(service.send(org.mockito.ArgumentMatchers.any(Notification.class), eq(Encoding.AES128GCM)))
                .thenReturn(response(status, null));
        WebPushGateway gateway = gateway(service);

        PushNotificadorPort.ResultadoEnvio result = gateway.enviar(alerta(), subscription());

        assertThat(result.exitoso()).isTrue();
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(service).send(notification.capture(), eq(Encoding.AES128GCM));
        String payload = new String(notification.getValue().getPayload(), StandardCharsets.UTF_8);
        assertThat(payload).contains("Prueba Web Push", "Mensaje de prueba", "alertaId");
    }

    @Test
    void clasifica403ComoConfiguracionNoReintentable() throws Exception {
        PushService service = mock(PushService.class);
        when(service.send(org.mockito.ArgumentMatchers.any(Notification.class), eq(Encoding.AES128GCM)))
                .thenReturn(response(403, "crypto-key header invalid"));

        PushNotificadorPort.ResultadoEnvio result = gateway(service).enviar(alerta(), subscription());

        assertThat(result.exitoso()).isFalse();
        assertThat(result.codigo()).isEqualTo("WEB_PUSH_HTTP_403");
        assertThat(result.reintentable()).isFalse();
        assertThat(result.suscripcionInvalida()).isFalse();
        assertThat(result.error()).contains("WEB_PUSH_HTTP_403", "crypto-key header invalid");
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 410})
    void invalidaSuscripcionPara404Y410(int status) throws Exception {
        PushService service = mock(PushService.class);
        when(service.send(org.mockito.ArgumentMatchers.any(Notification.class), eq(Encoding.AES128GCM)))
                .thenReturn(response(status, null));

        PushNotificadorPort.ResultadoEnvio result = gateway(service).enviar(alerta(), subscription());

        assertThat(result.suscripcionInvalida()).isTrue();
        assertThat(result.reintentable()).isFalse();
        assertThat(result.codigo()).isEqualTo("WEB_PUSH_HTTP_" + status);
    }

    private WebPushGateway gateway(PushService service) {
        return new WebPushGateway("test-public", "test-private", "mailto:soporte@ganadero.bo", 60,
                "https://ganadero.app", new ObjectMapper(), service);
    }

    private BasicHttpResponse response(int status, String body) {
        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1), status, "test"));
        if (body != null) response.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        return response;
    }

    private Alerta alerta() {
        Instant now = Instant.now();
        return new Alerta(UUID.randomUUID(), UUID.randomUUID(), null, TipoAlerta.SISTEMA_REQUIERE_ATENCION,
                "Prueba Web Push", "Mensaje de prueba", SeveridadAlerta.INFO, now, null, "PRUEBA", UUID.randomUUID(),
                EstadoAlerta.PENDIENTE, Map.of(), null, null, null, null, null, null, null, 0, null, now, now,
                "prueba:" + UUID.randomUUID());
    }

    private SuscripcionPush subscription() throws Exception {
        return new SuscripcionPush(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "https://fcm.googleapis.com/fcm/send/test", vapidKeys().publicKey(), "dGVzdC1hdXRo",
                "Android", "Chrome", true, Instant.now(), Instant.now(), Instant.now());
    }

    private VapidKeys vapidKeys() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return new VapidKeys(encoder.encodeToString(Utils.encode((ECPublicKey) pair.getPublic())),
                encoder.encodeToString(Utils.encode((ECPrivateKey) pair.getPrivate())));
    }

    private record VapidKeys(String publicKey, String privateKey) {}
}
