package bo.com.ganadero.integraciones.telegram;

import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integraciones/telegram")
public class TelegramIntegrationController {

    private final TelegramIntegrationService service;
    private final FirebaseRelaySyncService syncService;
    private final UserContext context;

    public TelegramIntegrationController(
            TelegramIntegrationService service,
            FirebaseRelaySyncService syncService,
            UserContext context) {
        this.service = service;
        this.syncService = syncService;
        this.context = context;
    }

    @GetMapping
    public ApiResponse<ConfiguracionTelegram> consultar(HttpServletRequest request) {
        CurrentUser user = context.currentUser();
        return ok(service.consultar(user.empresaId()), request);
    }

    @PutMapping
    public ApiResponse<ConfiguracionTelegram> guardar(
            @RequestBody TelegramIntegrationService.GuardarTelegramConfigRequest body,
            HttpServletRequest request) {
        CurrentUser user = context.requirePermission("SANIDAD_CONFIGURAR");
        return ok(service.guardar(user.empresaId(), body), request);
    }

    @GetMapping("/estado")
    public ApiResponse<TelegramIntegrationService.EstadoIntegracionTelegram> estado(HttpServletRequest request) {
        CurrentUser user = context.currentUser();
        return ok(service.consultarEstado(user.empresaId()), request);
    }

    @PostMapping("/test")
    public ApiResponse<TelegramIntegrationService.ResultadoEnvioTelegram> probar(
            @RequestBody(required = false) TelegramIntegrationService.EnviarPruebaRequest body,
            HttpServletRequest request) {
        CurrentUser user = context.requirePermission("SANIDAD_CONFIGURAR");
        return ok(service.enviarMensajePrueba(user.empresaId(), body), request);
    }

    @PostMapping("/sincronizar")
    public ApiResponse<Integer> sincronizar(HttpServletRequest request) {
        context.requirePermission("SANIDAD_CONFIGURAR");
        int cantidad = syncService.sincronizarPendientes();
        return ok(cantidad, request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object id = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, id == null ? "unknown" : id.toString());
    }
}
