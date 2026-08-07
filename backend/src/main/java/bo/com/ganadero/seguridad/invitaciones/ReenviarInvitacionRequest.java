package bo.com.ganadero.seguridad.invitaciones;

import jakarta.validation.constraints.PositiveOrZero;

public record ReenviarInvitacionRequest(@PositiveOrZero long version) {
}
