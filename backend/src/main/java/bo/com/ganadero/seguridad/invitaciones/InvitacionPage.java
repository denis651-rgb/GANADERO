package bo.com.ganadero.seguridad.invitaciones;

import java.util.List;

public record InvitacionPage(List<InvitacionResponse> items, long total, int page, int size) {
}
