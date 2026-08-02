package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.application.*;import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/roles")public class RolesController{
 private final ListarRolesUseCase list;private final ListarPermisosUseCase permissions;private final CrearRolUseCase create;private final ActualizarRolUseCase update;private final ActualizarPermisosRolUseCase updatePermissions;
 public RolesController(ListarRolesUseCase list,ListarPermisosUseCase permissions,CrearRolUseCase create,ActualizarRolUseCase update,ActualizarPermisosRolUseCase updatePermissions){this.list=list;this.permissions=permissions;this.create=create;this.update=update;this.updatePermissions=updatePermissions;}
 @GetMapping public ApiResponse<List<RolResponse>> list(HttpServletRequest r){return ok(list.execute().stream().map(RolResponse::from).toList(),r);}
 @GetMapping("/permisos")public ApiResponse<List<RolResponse.PermisoResponse>> permissions(HttpServletRequest r){return ok(permissions.execute().stream().map(p->new RolResponse.PermisoResponse(p.id(),p.codigo(),p.nombre(),p.modulo())).toList(),r);}
 @PostMapping public ApiResponse<RolResponse> create(@Valid @RequestBody CrearRolRequest b,HttpServletRequest r){return ok(RolResponse.from(create.execute(b.codigo(),b.nombre(),b.descripcion())),r);}
 @GetMapping("/{id}")public ApiResponse<RolResponse> get(@PathVariable UUID id,HttpServletRequest r){return ok(RolResponse.from(list.get(id)),r);}
 @PatchMapping("/{id}")public ApiResponse<RolResponse> update(@PathVariable UUID id,@Valid @RequestBody ActualizarRolRequest b,HttpServletRequest r){return ok(RolResponse.from(update.execute(id,b.nombre(),b.descripcion(),b.activo(),b.version())),r);}
 @PutMapping("/{id}/permisos")public ApiResponse<RolResponse> permissions(@PathVariable UUID id,@Valid @RequestBody IdsRequest b,HttpServletRequest r){return ok(RolResponse.from(updatePermissions.execute(id,b.ids(),b.version())),r);}
 private <T>ApiResponse<T> ok(T data,HttpServletRequest r){Object v=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(data,v==null?"unknown":v.toString());}}
