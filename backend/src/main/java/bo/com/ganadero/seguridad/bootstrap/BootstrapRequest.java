package bo.com.ganadero.seguridad.bootstrap;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BootstrapRequest(@Valid @NotNull Empresa empresa,
                               @Valid @NotNull Propietario propietario,
                               @Valid @NotNull Propiedad propiedadInicial) {
    public record Empresa(@NotBlank @Size(max=30) String codigo,
        @NotBlank @Size(max=180) String razonSocial, @NotBlank @Size(max=180) String nombreComercial,
        @Size(max=40) String nit, @Size(max=40) String telefono,
        @NotBlank @Email @Size(max=180) String email, @Size(max=300) String direccion,
        @Size(max=60) String zonaHoraria, @Pattern(regexp="[A-Z]{3}") String moneda) {}
    public record Propietario(@NotBlank @Email @Size(max=180) String email,
        @NotBlank @Size(max=120) String nombres, @NotBlank @Size(max=120) String apellidos,
        @Size(max=40) String telefono, @Size(max=120) String cargo) {}
    public record Propiedad(@NotBlank @Size(max=60) String codigo, @NotBlank @Size(max=160) String nombre,
        @Size(max=1000) String descripcion, @Size(max=120) String departamento,
        @Size(max=120) String municipio, @Size(max=160) String localidad,
        @Size(max=500) String direccionReferencia, @DecimalMin("0.0") BigDecimal superficieHa) {}
}
