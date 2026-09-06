package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.PlanSanitarioService;
import bo.com.ganadero.sanidad.domain.EstadoEventoCalendario;
import bo.com.ganadero.sanidad.domain.EventoCalendarioSanitario;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sanidad/calendario")
public class CalendarioSanitarioController {
    private final PlanSanitarioService service;

    public CalendarioSanitarioController(PlanSanitarioService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<EventoCalendarioSanitario>> listar(@RequestParam(required = false) EstadoEventoCalendario estado,
                                                        @RequestParam(required = false) UUID animalId,
                                                        HttpServletRequest r) {
        Object c = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(service.calendario(estado, animalId), c == null ? "unknown" : c.toString());
    }
}
