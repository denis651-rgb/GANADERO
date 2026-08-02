package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.application.CrearMiembroCommand;import jakarta.validation.constraints.*;import java.util.*;
public record CrearMiembroRequest(@NotNull UUID usuarioId,@NotBlank @Size(max=120)String nombres,@NotBlank @Size(max=120)String apellidos,
 @Size(max=40)String telefono,@Size(max=120)String cargo,boolean accesoTodasPropiedades,@NotEmpty Set<UUID> roles){
 CrearMiembroCommand toCommand(){return new CrearMiembroCommand(usuarioId,nombres,apellidos,telefono,cargo,accesoTodasPropiedades,roles);}}
