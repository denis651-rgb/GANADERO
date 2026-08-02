package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.application.ActualizarMiembroCommand;import jakarta.validation.constraints.*;
public record ActualizarMiembroRequest(@Size(max=120)String nombres,@Size(max=120)String apellidos,@Size(max=40)String telefono,
 @Size(max=500)String avatarPath,@Size(max=120)String cargo,Boolean accesoTodasPropiedades,@NotNull Long perfilVersion,@NotNull Long version){
 ActualizarMiembroCommand toCommand(){return new ActualizarMiembroCommand(nombres,apellidos,telefono,avatarPath,cargo,accesoTodasPropiedades,perfilVersion,version);}}
