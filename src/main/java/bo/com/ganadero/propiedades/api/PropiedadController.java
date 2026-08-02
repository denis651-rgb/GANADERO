package bo.com.ganadero.propiedades.api;
import bo.com.ganadero.propiedades.application.PropiedadService; import bo.com.ganadero.shared.api.ApiResponse; import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1") public class PropiedadController {
 private final PropiedadService service; public PropiedadController(PropiedadService s){service=s;}
 @GetMapping("/propiedades") ApiResponse<List<PropiedadResponse>> list(HttpServletRequest r){return ok(service.list().stream().map(PropiedadResponse::from).toList(),r);}
 @PostMapping("/propiedades") ApiResponse<PropiedadResponse> create(@Valid @RequestBody CrearPropiedadRequest b,HttpServletRequest r){return ok(PropiedadResponse.from(service.create(b.command())),r);}
 @GetMapping("/propiedades/{id}") ApiResponse<PropiedadResponse> get(@PathVariable UUID id,HttpServletRequest r){return ok(PropiedadResponse.from(service.get(id)),r);}
 @PatchMapping("/propiedades/{id}") ApiResponse<PropiedadResponse> update(@PathVariable UUID id,@Valid @RequestBody ActualizarPropiedadRequest b,HttpServletRequest r){return ok(PropiedadResponse.from(service.update(id,b.command())),r);}
 @GetMapping("/propiedades/{id}/sectores") ApiResponse<List<SectorResponse>> sectors(@PathVariable UUID id,HttpServletRequest r){return ok(service.sectors(id).stream().map(SectorResponse::from).toList(),r);}
 @PostMapping("/propiedades/{id}/sectores") ApiResponse<SectorResponse> createSector(@PathVariable UUID id,@Valid @RequestBody CrearSectorRequest b,HttpServletRequest r){return ok(SectorResponse.from(service.createSector(id,b.command())),r);}
 @PatchMapping("/sectores/{id}") ApiResponse<SectorResponse> updateSector(@PathVariable UUID id,@Valid @RequestBody ActualizarSectorRequest b,HttpServletRequest r){return ok(SectorResponse.from(service.updateSector(id,b.command())),r);}
 private <T> ApiResponse<T> ok(T data,HttpServletRequest r){Object c=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(data,c==null?"unknown":c.toString());}
}
