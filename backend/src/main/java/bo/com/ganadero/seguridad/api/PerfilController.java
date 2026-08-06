package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.error.*;import bo.com.ganadero.shared.security.*;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;import org.springframework.web.bind.annotation.*;import java.time.*;import java.util.*;
@RestController @RequestMapping("/api/v1/perfil") public class PerfilController{
 private final JdbcClient jdbc;private final UserContext context;public PerfilController(JdbcClient jdbc,UserContext context){this.jdbc=jdbc;this.context=context;}
 @GetMapping @Transactional(readOnly=true) public ApiResponse<PerfilResponse> get(HttpServletRequest request){CurrentUser user=context.currentUser();return ok(load(user),request);}
 @PatchMapping @Transactional public ApiResponse<PerfilResponse> update(@Valid @RequestBody UpdatePerfil body,HttpServletRequest request){CurrentUser user=context.currentUser();
  int changed=jdbc.sql("""
   update seguridad.perfiles_usuario set nombres=coalesce(:nombres,nombres),apellidos=coalesce(:apellidos,apellidos),
   telefono=coalesce(:telefono,telefono),updated_at=now(),updated_by=:actor,version=version+1 where id=:id and version=:version
   """)
   .param("nombres",body.nombres()).param("apellidos",body.apellidos()).param("telefono",body.telefono()).param("actor",user.userId())
   .param("id",user.userId()).param("version",body.version()).update();if(changed==0)throw new BusinessException(ErrorCode.VERSION_CONFLICT);return ok(load(user),request);}
 private PerfilResponse load(CurrentUser user){return jdbc.sql("""
   select p.id,p.email,p.nombres,p.apellidos,p.telefono,p.avatar_path,p.ultimo_acceso_at,p.version,
   m.cargo,m.estado,e.id empresa_id,e.nombre_comercial from seguridad.perfiles_usuario p join seguridad.miembros_empresa m on m.usuario_id=p.id
   join core.empresas e on e.id=m.empresa_id where p.id=:usuario and m.empresa_id=:empresa
   """).param("usuario",user.userId()).param("empresa",user.empresaId())
   .query((rs,row)->new PerfilResponse(rs.getObject("id",UUID.class),rs.getString("email"),rs.getString("nombres"),rs.getString("apellidos"),
    rs.getString("telefono"),rs.getString("avatar_path"),rs.getString("cargo"),rs.getString("estado"),rs.getObject("empresa_id",UUID.class),
    rs.getString("nombre_comercial"),user.roles(),user.permisos(),user.propiedadesPermitidas(),rs.getObject("ultimo_acceso_at",OffsetDateTime.class),rs.getLong("version"))).optional()
   .orElseThrow(()->new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));}
 private <T>ApiResponse<T> ok(T data,HttpServletRequest request){Object value=request.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(data,value==null?"unknown":value.toString());}
 public record UpdatePerfil(@Size(max=120)String nombres,@Size(max=120)String apellidos,@Size(max=40)String telefono,@PositiveOrZero long version){}
 public record PerfilResponse(UUID usuarioId,String email,String nombres,String apellidos,String telefono,String avatarPath,String cargo,String estado,
  UUID empresaId,String empresa,Set<String> roles,Set<String> permisos,Set<UUID> propiedades,OffsetDateTime ultimoAcceso,long version){}
}
