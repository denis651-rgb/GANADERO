package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.domain.UsuarioActual;import java.util.*;
public record AuthMeResponse(UUID usuarioId,String nombres,String apellidos,EmpresaResumen empresa,
        Set<String> roles,Set<String> permisos,Set<UUID> propiedadesPermitidas){
 public record EmpresaResumen(UUID id,String nombre){}
 static AuthMeResponse from(UsuarioActual u){return new AuthMeResponse(u.usuarioId(),u.nombres(),u.apellidos(),new EmpresaResumen(u.empresaId(),u.empresaNombre()),u.roles(),u.permisos(),u.propiedadesPermitidas());}}
