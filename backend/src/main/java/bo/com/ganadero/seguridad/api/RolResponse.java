package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.domain.*;import java.util.*;
public record RolResponse(UUID id,UUID empresaId,String codigo,String nombre,String descripcion,boolean sistema,boolean activo,long version,Set<PermisoResponse> permisos){
 public record PermisoResponse(UUID id,String codigo,String nombre,String modulo){}
 static RolResponse from(Rol r){Set<PermisoResponse> p=new LinkedHashSet<>();r.permisos().stream().sorted(java.util.Comparator.comparing(Permiso::codigo)).forEach(x->p.add(new PermisoResponse(x.id(),x.codigo(),x.nombre(),x.modulo())));return new RolResponse(r.id(),r.empresaId(),r.codigo(),r.nombre(),r.descripcion(),r.sistema(),r.activo(),r.version(),p);}}
