package bo.com.ganadero.shared.status;

import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private static final List<String> MODULES = List.of(
            "shared", "seguridad", "empresas", "propiedades", "animales",
            "potreros", "lotes", "movimientos", "pesajes", "reproduccion",
            "sanidad", "inventario", "alimentacion", "comercial", "finanzas",
            "archivos", "alertas", "reportes", "sincronizacion", "auditoria"
    );

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        return ApiResponse.success(Map.of(
                "application", "GANADERO",
                "architecture", "MODULAR_MONOLITH",
                "phase", "FASE_1_F1_5",
                "moduleCount", MODULES.size(),
                "modules", MODULES
        ), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? "unknown" : value.toString();
    }
}
