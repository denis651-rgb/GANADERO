package bo.com.ganadero.sync.api;

import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/operaciones")
public class OperacionesLoteController {
    private final bo.com.ganadero.sync.application.SyncService service;

    public OperacionesLoteController(bo.com.ganadero.sync.application.SyncService service) {
        this.service = service;
    }

    @PostMapping("/lote")
    ApiResponse<LoteResponse> lote(@Valid @RequestBody LoteRequest body, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(service.lote(body.operaciones()), value == null ? "unknown" : value.toString());
    }
}
