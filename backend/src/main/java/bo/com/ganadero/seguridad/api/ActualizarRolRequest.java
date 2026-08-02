package bo.com.ganadero.seguridad.api;import jakarta.validation.constraints.*;
public record ActualizarRolRequest(@Size(max=120)String nombre,@Size(max=500)String descripcion,Boolean activo,@NotNull Long version){}
