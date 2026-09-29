package bo.com.ganadero.configuracion.api;

import bo.com.ganadero.configuracion.application.ConfiguracionService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion")
public class ConfiguracionController {

    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<ConfiguracionResponse> get(HttpServletRequest request) {
        return ok(ConfiguracionResponse.from(service.get()), request);
    }

    @PatchMapping
    public ApiResponse<ConfiguracionResponse> update(
            @Valid @RequestBody ActualizarConfiguracionRequest body,
            HttpServletRequest request) {
        return ok(ConfiguracionResponse.from(service.update(body.command())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlationId == null ? "unknown" : correlationId.toString());
    }
}

