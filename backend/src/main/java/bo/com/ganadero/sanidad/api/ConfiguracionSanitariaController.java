package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.ConfiguracionSanitariaService;
import bo.com.ganadero.sanidad.application.ConfiguracionSanitariaService.Configuracion;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sanidad/configuracion")
public class ConfiguracionSanitariaController {
    private final ConfiguracionSanitariaService service;
    public ConfiguracionSanitariaController(ConfiguracionSanitariaService service) { this.service=service; }
    @GetMapping public ApiResponse<Configuracion> consultar(HttpServletRequest r) { return ok(service.consultar(),r); }
    @PutMapping public ApiResponse<Configuracion> guardar(@RequestBody Configuracion c,HttpServletRequest r) { return ok(service.guardar(c),r); }
    private ApiResponse<Configuracion> ok(Configuracion c,HttpServletRequest r) {
        Object id=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(c,id==null?"unknown":id.toString());
    }
}
