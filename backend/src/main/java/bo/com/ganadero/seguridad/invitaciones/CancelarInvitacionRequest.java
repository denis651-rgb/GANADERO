package bo.com.ganadero.seguridad.invitaciones;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CancelarInvitacionRequest(@Size(max = 300) String motivo,
                                        @PositiveOrZero long version) {
}
