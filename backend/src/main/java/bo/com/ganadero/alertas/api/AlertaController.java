package bo.com.ganadero.alertas.api;

import bo.com.ganadero.alertas.application.AlertaQueryService;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final AlertaQueryService service;

    public AlertaController(AlertaQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Alerta>> listar(
            @RequestParam(required = false) EstadoAlerta estado,
            @RequestParam(required = false) TipoAlerta tipo,
            @RequestParam(required = false) UUID animalId,
            HttpServletRequest request) {
        return ok(service.listar(estado, tipo, animalId), request);
    }

    @GetMapping("/{id}")
    public ApiResponse<Alerta> obtener(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.obtener(id), request);
    }

    @GetMapping("/no-leidas/count")
    public ApiResponse<AlertaQueryService.ConteoAlertas> count(HttpServletRequest request) {
        return ok(service.noLeidas(), request);
    }

    @GetMapping("/resumen")
    public ApiResponse<AlertaQueryService.ResumenAlertas> resumen(HttpServletRequest request) {
        return ok(service.resumen(), request);
    }

    @GetMapping("/pendientes-notificar")
    public ApiResponse<List<Alerta>> pendientesNotificar(
            @RequestParam(defaultValue = "50") int limite,
            HttpServletRequest request) {
        return ok(service.pendientesParaNotificar(limite), request);
    }

    @PostMapping("/{id}/marcar-enviada")
    public ApiResponse<Alerta> marcarEnviada(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.marcarEnviada(id), request);
    }

    @PostMapping("/{id}/marcar-error")
    public ApiResponse<Alerta> marcarError(
            @PathVariable UUID id,
            @RequestParam(required = false) String error,
            HttpServletRequest request) {
        return ok(service.marcarError(id, error), request);
    }

    @PostMapping("/{id}/atender")
    public ApiResponse<Alerta> atender(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.atender(id), request);
    }

    @PostMapping("/{id}/resolver")
    public ApiResponse<Alerta> resolver(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.resolver(id), request);
    }

    @PostMapping("/procesar-vencidas")
    public ApiResponse<Integer> procesar(
            @RequestParam(defaultValue = "100") int limite,
            HttpServletRequest request) {
        return ok(service.procesarVencidas(limite), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlationId == null ? "unknown" : correlationId.toString());
    }
}

