package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.api.CrearSuscripcionPushRequest;
import bo.com.ganadero.alertas.api.PreferenciasNotificacionRequest;
import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushSubscriptionService {
    private final SuscripcionPushRepository repo;
    private final UserContext context;
    private final String vapidPublicKey;

    public PushSubscriptionService(SuscripcionPushRepository repo, UserContext context,
                                   @Value("${ganadero.push.vapid-public-key:}") String vapidPublicKey) {
        this.repo = repo;
        this.context = context;
        this.vapidPublicKey = clean(vapidPublicKey);
    }

    @Transactional
    public SuscripcionPush crear(CrearSuscripcionPushRequest request, String userAgent) {
        CurrentUser user = context.requirePermission("ALERTA_VER");
        return repo.guardar(new SuscripcionPush(UUID.randomUUID(), user.empresaId(), user.userId(),
                request.endpoint(), request.keys().p256dh(), request.keys().auth(), request.dispositivoNombre(),
                userAgent, true, null, null, null));
    }

    @Transactional(readOnly = true)
    public List<SuscripcionPush> listar() {
        CurrentUser user = context.requirePermission("ALERTA_VER");
        return repo.listar(user.empresaId(), user.userId());
    }

    @Transactional
    public void eliminar(UUID id) {
        CurrentUser user = context.requirePermission("ALERTA_VER");
        repo.desactivar(id, user.empresaId(), user.userId());
    }

    @Transactional(readOnly = true)
    public PreferenciasNotificacion preferencias() {
        CurrentUser user = context.requirePermission("ALERTA_VER");
        return repo.preferencias(user.empresaId(), user.userId());
    }

    @Transactional
    public PreferenciasNotificacion preferencias(PreferenciasNotificacionRequest request) {
        CurrentUser user = context.requirePermission("ALERTA_VER");
        return repo.guardarPreferencias(new PreferenciasNotificacion(user.empresaId(), user.userId(),
                request.reproduccion(), request.sanidad(), request.tratamientos(), request.pesajes(),
                request.movimientos(), request.inventario(), request.sistema(), request.casosCriticos(),
                request.criticas(), request.urgentes(), request.recordatorios()));
    }

    public Map<String, String> clavePublica() {
        return Map.of("publicKey", vapidPublicKey);
    }

    private static String clean(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")
                ? trimmed.substring(1, trimmed.length() - 1).trim() : trimmed;
    }
}
