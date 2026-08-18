package bo.com.ganadero.alertas.api;

import bo.com.ganadero.alertas.application.PushSubscriptionService;
import bo.com.ganadero.alertas.application.PushTestService;
import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alertas")
public class PushSubscriptionController {
    private final PushSubscriptionService service;
    private final PushTestService pushTestService;

    public PushSubscriptionController(PushSubscriptionService service, PushTestService pushTestService) {
        this.service = service;
        this.pushTestService = pushTestService;
    }

    @GetMapping("/push/public-key")
    ApiResponse<Map<String, String>> key(HttpServletRequest request) {
        return ok(service.clavePublica(), request);
    }

    @PostMapping("/push/suscripciones")
    ApiResponse<PushDeviceResponse> crear(@Valid @RequestBody CrearSuscripcionPushRequest body,
                                          @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                          HttpServletRequest request) {
        return ok(PushDeviceResponse.from(service.crear(body, userAgent)), request);
    }

    @GetMapping("/push/suscripciones")
    ApiResponse<List<PushDeviceResponse>> listar(HttpServletRequest request) {
        return ok(service.listar().stream().map(PushDeviceResponse::from).toList(), request);
    }

    @DeleteMapping("/push/suscripciones/{id}")
    ApiResponse<Void> eliminar(@PathVariable UUID id, HttpServletRequest request) {
        service.eliminar(id);
        return ok(null, request);
    }

    @PostMapping("/notificaciones/push/prueba")
    ApiResponse<PushTestResponse> probar(@Valid @RequestBody PushTestRequest body, HttpServletRequest request) {
        return ok(pushTestService.enviar(body), request);
    }

    @GetMapping("/configuracion")
    ApiResponse<PreferenciasNotificacion> config(HttpServletRequest request) {
        return ok(service.preferencias(), request);
    }

    @PutMapping("/configuracion")
    ApiResponse<PreferenciasNotificacion> config(@RequestBody PreferenciasNotificacionRequest body,
                                                  HttpServletRequest request) {
        return ok(service.preferencias(body), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlation = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlation == null ? "unknown" : correlation.toString());
    }
}
