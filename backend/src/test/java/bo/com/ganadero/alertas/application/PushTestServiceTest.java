package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.api.PushTestRequest;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushTestServiceTest {
    @Test
    void enviaSoloAlDispositivoSolicitadoDelUsuarioActual() {
        UUID empresa = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        SuscripcionPush subscription = subscription(empresa, usuario);
        SuscripcionPushRepository repository = mock(SuscripcionPushRepository.class);
        PushNotificadorPort notifier = mock(PushNotificadorPort.class);
        UserContext context = mock(UserContext.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        when(context.requirePermission("ALERTA_CONFIGURAR")).thenReturn(user(empresa, usuario));
        when(repository.listar(empresa, usuario)).thenReturn(List.of(subscription));
        when(notifier.enviar(any(), any())).thenReturn(PushNotificadorPort.ResultadoEnvio.ok());

        var result = new PushTestService(repository, notifier, context, events).enviar(
                new PushTestRequest(subscription.id(), "Prueba", "Mensaje"));

        assertThat(result.ok()).isTrue();
        assertThat(result.estado()).isEqualTo("ENVIADA");
        verify(notifier).enviar(any(), org.mockito.ArgumentMatchers.eq(subscription));
        verify(repository, never()).desactivarTodas(any(), any());
        verify(events).publishEvent(any(Object.class));
    }

    @Test
    void desactivaLaSuscripcionCuandoElProveedorResponde410() {
        UUID empresa = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        SuscripcionPush subscription = subscription(empresa, usuario);
        SuscripcionPushRepository repository = mock(SuscripcionPushRepository.class);
        PushNotificadorPort notifier = mock(PushNotificadorPort.class);
        UserContext context = mock(UserContext.class);
        when(context.requirePermission("ALERTA_CONFIGURAR")).thenReturn(user(empresa, usuario));
        when(repository.listar(empresa, usuario)).thenReturn(List.of(subscription));
        when(notifier.enviar(any(), any())).thenReturn(PushNotificadorPort.ResultadoEnvio.http(
                410, "WEB_PUSH_HTTP_410: expirada"));

        var result = new PushTestService(repository, notifier, context, mock(ApplicationEventPublisher.class))
                .enviar(new PushTestRequest(subscription.id(), "Prueba", "Mensaje"));

        assertThat(result.ok()).isFalse();
        assertThat(result.codigo()).isEqualTo("WEB_PUSH_HTTP_410");
        verify(repository).desactivarTodas(subscription.id(), empresa);
    }

    private CurrentUser user(UUID empresa, UUID usuario) {
        return new CurrentUser(usuario, empresa, UUID.randomUUID(), Set.of("ADMINISTRADOR"),
                Set.of("ALERTA_CONFIGURAR"), Set.of(), true);
    }

    private SuscripcionPush subscription(UUID empresa, UUID usuario) {
        return new SuscripcionPush(UUID.randomUUID(), empresa, usuario, "https://fcm.googleapis.com/test",
                "public", "auth", "Android", "Chrome", true, Instant.now(), Instant.now(), Instant.now());
    }
}
