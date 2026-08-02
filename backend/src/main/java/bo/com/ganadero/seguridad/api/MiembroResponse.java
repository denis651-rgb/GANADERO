package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.domain.*;import java.time.*;import java.util.*;
public record MiembroResponse(UUID id,UUID usuarioId,String nombres,String apellidos,String telefono,String avatarPath,
 String cargo,EstadoMiembro estado,LocalDate fechaIngreso,boolean accesoTodasPropiedades,long perfilVersion,long version,
 Set<RolResumen> roles,Set<UUID> propiedadesPermitidas){public record RolResumen(UUID id,String codigo,String nombre){}
 static MiembroResponse from(MiembroEmpresa m){Set<RolResumen> roles=new LinkedHashSet<>();m.roles().stream().sorted(java.util.Comparator.comparing(Rol::codigo)).forEach(r->roles.add(new RolResumen(r.id(),r.codigo(),r.nombre())));return new MiembroResponse(m.id(),m.perfil().id(),m.perfil().nombres(),m.perfil().apellidos(),m.perfil().telefono(),m.perfil().avatarPath(),m.cargo(),m.estado(),m.fechaIngreso(),m.accesoTodasPropiedades(),m.perfil().version(),m.version(),roles,m.propiedadesPermitidas());}}
