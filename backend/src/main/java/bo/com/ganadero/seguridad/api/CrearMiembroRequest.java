package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.application.CrearMiembroCommand;import jakarta.validation.constraints.*;import java.util.*;
public record CrearMiembroRequest(@NotBlank @Email @Size(max=180)String email,
 @NotBlank @Size(max=120)String nombres,@NotBlank @Size(max=120)String apellidos,@Size(max=40)String telefono,
 @Size(max=120)String cargo,boolean accesoTodasPropiedades,@NotEmpty Set<UUID> roles,Set<UUID> propiedades){
 CrearMiembroCommand toCommand(){return new CrearMiembroCommand(email,nombres,apellidos,telefono,cargo,accesoTodasPropiedades,roles,
  propiedades==null?Set.of():propiedades);}}
