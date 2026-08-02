package bo.com.ganadero.seguridad.api;import jakarta.validation.constraints.*;
public record CrearRolRequest(@NotBlank @Pattern(regexp="[A-Z0-9_]+") @Size(max=80)String codigo,
 @NotBlank @Size(max=120)String nombre,@Size(max=500)String descripcion){}
