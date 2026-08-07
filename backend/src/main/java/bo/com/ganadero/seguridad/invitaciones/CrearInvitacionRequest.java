package bo.com.ganadero.seguridad.invitaciones;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearInvitacionRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 120) String cargo) {
}
