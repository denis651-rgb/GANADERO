package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.ClinicaService;
import bo.com.ganadero.sanidad.domain.AplicacionTratamiento;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoint operativo corto especificado para la PWA. */
@RestController
@RequestMapping("/api/v1/tratamientos")
public class AplicacionTratamientoController {

    private final ClinicaService service;

    public AplicacionTratamientoController(ClinicaService service) {
        this.service = service;
    }

    @PostMapping("/{tratamientoId}/aplicaciones/{aplicacionId}/aplicar")
    public ApiResponse<AplicacionTratamiento> aplicar(
            @PathVariable UUID tratamientoId,
            @PathVariable UUID aplicacionId,
            @Valid @RequestBody AplicarTratamientoRequest body,
            HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(
                service.aplicar(tratamientoId, aplicacionId, body.command()),
                correlationId == null ? "unknown" : correlationId.toString()
        );
    }
}

