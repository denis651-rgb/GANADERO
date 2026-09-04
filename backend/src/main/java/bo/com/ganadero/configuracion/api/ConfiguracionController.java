package bo.com.ganadero.configuracion.api;
import bo.com.ganadero.configuracion.application.ConfiguracionService; import bo.com.ganadero.shared.api.ApiResponse; import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/configuracion") public class ConfiguracionController {
 private final ConfiguracionService service; public ConfiguracionController(ConfiguracionService s){service=s;}
 @GetMapping ApiResponse<ConfiguracionResponse> get(HttpServletRequest r){return ok(ConfiguracionResponse.from(service.get()),r);}
 @PatchMapping ApiResponse<ConfiguracionResponse> update(@Valid @RequestBody ActualizarConfiguracionRequest b,HttpServletRequest r){return ok(ConfiguracionResponse.from(service.update(b.command())),r);}
 private <T> ApiResponse<T> ok(T data,HttpServletRequest r){Object c=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(data,c==null?"unknown":c.toString());}
}
