package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.api.PushTestRequest;
import bo.com.ganadero.alertas.api.PushTestResponse;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class PushTestService {
    private final SuscripcionPushRepository suscripciones;
    private final PushNotificadorPort notificador;
    private final UserContext context;
    private final ApplicationEventPublisher events;

    public PushTestService(SuscripcionPushRepository suscripciones, PushNotificadorPort notificador,
                           UserContext context, ApplicationEventPublisher events) {
        this.suscripciones = suscripciones;
        this.notificador = notificador;
        this.context = context;
        this.events = events;
    }

    @Transactional
    public PushTestResponse enviar(PushTestRequest request) {
        CurrentUser user = context.requirePermission("ALERTA_CONFIGURAR");
        SuscripcionPush subscription = suscripciones.listar(user.empresaId(), user.userId()).stream()
                .filter(item -> item.id().equals(request.suscripcionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PUSH_SUBSCRIPTION_NOT_FOUND));

        UUID pruebaId = UUID.randomUUID();
        Instant now = Instant.now();
        Alerta prueba = new Alerta(pruebaId, user.empresaId(), null, TipoAlerta.SISTEMA_REQUIERE_ATENCION,
                request.titulo().trim(), request.mensaje().trim(), SeveridadAlerta.INFO, now, null,
                "PRUEBA_PUSH", pruebaId, EstadoAlerta.PENDIENTE, Map.of("prueba", true),
                null, null, null, null, null, null, null, 0, null, now, now,
                "prueba-push:" + pruebaId);

        PushNotificadorPort.ResultadoEnvio result = notificador.enviar(prueba, subscription);
        if (result.suscripcionInvalida()) {
            suscripciones.desactivarTodas(subscription.id(), user.empresaId());
        }
        events.publishEvent(new AlertasAuditEvent(user.empresaId(), user.userId(), "PROBAR_PUSH",
                "SUSCRIPCION_PUSH", subscription.id(),
                Map.of("estado", result.exitoso() ? "ENVIADA" : "ERROR",
                        "codigo", result.codigo() == null ? "OK" : result.codigo()), now));

        return new PushTestResponse(result.exitoso(), result.exitoso() ? "ENVIADA" : "ERROR",
                result.codigo(), result.exitoso() ? "Notificación enviada" : result.error());
    }
}
