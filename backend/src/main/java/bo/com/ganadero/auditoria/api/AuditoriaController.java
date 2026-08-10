package bo.com.ganadero.auditoria.api;

import bo.com.ganadero.auditoria.application.AuditoriaService;
import bo.com.ganadero.auditoria.domain.AuditoriaFilter;
import bo.com.ganadero.auditoria.domain.AuditPage;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<AuditPageResponse> list(
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) UUID propiedadId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            HttpServletRequest request) {
        AuditPage result = service.list(new AuditoriaFilter(usuarioId, modulo, accion, entidad, propiedadId,
                correlationId, desde, hasta, page, size));
        return ok(AuditPageResponse.from(result), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
