package bo.com.ganadero.sanidad.api;
import bo.com.ganadero.sanidad.application.HistorialDeclaradoService;
import bo.com.ganadero.sanidad.domain.AplicacionSanitaria;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Sibling de ClinicaController bajo el mismo prefijo /api/v1/sanidad: no vive ahí porque
 * AplicacionSanitaria se persiste vía JornadaSanitariaRepository (dominio de jornadas/
 * vacunación), no vía ClinicaRepository (casos clínicos/tratamientos).
 */
@RestController @RequestMapping("/api/v1/sanidad")
public class HistorialDeclaradoController {
    private final HistorialDeclaradoService service;

    public HistorialDeclaradoController(HistorialDeclaradoService service) {
        this.service = service;
    }

    @PostMapping("/aplicaciones/declaradas")
    ApiResponse<AplicacionSanitaria> registrar(@Valid @RequestBody RegistrarAplicacionDeclaradaRequest body, HttpServletRequest r) {
        return ok(service.registrar(body.command()), r);
    }

    private <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        Object c = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(d, c == null ? "unknown" : c.toString());
    }
}
