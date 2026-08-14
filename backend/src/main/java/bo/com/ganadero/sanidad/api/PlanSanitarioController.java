package bo.com.ganadero.sanidad.api;
import bo.com.ganadero.sanidad.application.*; import bo.com.ganadero.sanidad.domain.*; import bo.com.ganadero.shared.api.ApiResponse; import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import java.time.LocalDate; import java.util.*;
@RestController @RequestMapping("/api/v1/sanidad") public class PlanSanitarioController {
 private final PlanSanitarioService service; public PlanSanitarioController(PlanSanitarioService service){this.service=service;}
 @GetMapping("/enfermedades") ApiResponse<List<Enfermedad>> enfermedades(@RequestParam(defaultValue="false")boolean incluirInactivas,HttpServletRequest r){return ok(service.enfermedades(incluirInactivas),r);}
 @PostMapping("/enfermedades") ApiResponse<Enfermedad> crear(@Valid @RequestBody CrearEnfermedadRequest b,HttpServletRequest r){return ok(service.crearEnfermedad(b.command()),r);}
 @PatchMapping("/enfermedades/{id}/activo") ApiResponse<Enfermedad> estado(@PathVariable UUID id,@Valid @RequestBody CambiarActivoRequest b,HttpServletRequest r){return ok(service.estadoEnfermedad(id,b.activo()),r);}
 @GetMapping("/planes") ApiResponse<List<PlanSanitario>> planes(HttpServletRequest r){return ok(service.planes(),r);}
 @PostMapping("/planes") ApiResponse<PlanSanitario> crearPlan(@Valid @RequestBody CrearPlanRequest b,HttpServletRequest r){return ok(service.crearPlan(b.command()),r);}
 @PatchMapping("/planes/{id}/estado") ApiResponse<PlanSanitario> estadoPlan(@PathVariable UUID id,@Valid @RequestBody CambiarEstadoPlanRequest b,HttpServletRequest r){return ok(service.cambiarEstado(id,b.estado(),b.version()),r);}
 @GetMapping("/planes/{id}/items") ApiResponse<List<PlanSanitarioItem>> items(@PathVariable UUID id,@RequestParam(defaultValue="false")boolean incluirInactivos,HttpServletRequest r){return ok(service.items(id,incluirInactivos),r);}
 @PostMapping("/planes/{id}/items") ApiResponse<PlanSanitarioItem> crearItem(@PathVariable UUID id,@Valid @RequestBody CrearPlanItemRequest b,HttpServletRequest r){return ok(service.crearItem(id,b.command()),r);}
 @PatchMapping("/planes/{plan}/items/{id}/activo") ApiResponse<PlanSanitarioItem> estadoItem(@PathVariable UUID plan,@PathVariable UUID id,@Valid @RequestBody CambiarActivoRequest b,HttpServletRequest r){return ok(service.estadoItem(plan,id,b.activo(),b.version()),r);}
 @GetMapping("/planes/{plan}/items/{item}/proxima") ApiResponse<ProximaActividadSanitaria> proxima(@PathVariable UUID plan,@PathVariable UUID item,@RequestParam LocalDate fechaAplicacion,HttpServletRequest r){return ok(service.calcularProxima(plan,item,fechaAplicacion),r);}
 private <T> ApiResponse<T> ok(T d,HttpServletRequest r){Object c=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(d,c==null?"unknown":c.toString());}
}
