package bo.com.ganadero.alertas.api;

import bo.com.ganadero.alertas.application.InternalJobService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos exclusivos para Supabase Cron. No son de uso de usuarios:
 * se autentican con X-Ganadero-Cron-Secret en lugar de JWT de usuario.
 */
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {
    private final InternalJobService service;

    public InternalJobController(InternalJobService service) {
        this.service = service;
    }

    @PostMapping("/alertas/activar")
    public ApiResponse<Integer> activarAlertas(
            @RequestHeader(value = "X-Ganadero-Cron-Secret", required = false) String token,
            HttpServletRequest request) {
        return ok(service.activarAlertasVencidas(token), request);
    }

    @PostMapping("/notificaciones/procesar")
    public ApiResponse<Integer> procesarNotificaciones(
            @RequestHeader(value = "X-Ganadero-Cron-Secret", required = false) String token,
            HttpServletRequest request) {
        return ok(service.procesarNotificacionesPendientes(token), request);
    }

    @PostMapping("/alertas/pesajes/generar")
    public ApiResponse<Integer> generarAlertasPesajes(
            @RequestHeader(value = "X-Ganadero-Cron-Secret", required = false) String token,
            HttpServletRequest request) {
        return ok(service.generarAlertasPesajes(token), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlation = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlation == null ? "unknown" : correlation.toString());
    }
}
