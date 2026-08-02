package bo.com.ganadero.seguridad.application;
import java.util.Set;import java.util.UUID;
public record CrearMiembroCommand(String email,String nombres,String apellidos,String telefono,String cargo,
 boolean accesoTodasPropiedades,Set<UUID> roles,Set<UUID> propiedades){public CrearMiembroCommand{roles=Set.copyOf(roles);propiedades=Set.copyOf(propiedades);}}
