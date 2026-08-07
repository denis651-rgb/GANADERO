package bo.com.ganadero.seguridad.invitaciones;

import java.time.OffsetDateTime;

public record InvitacionFiltro(String estado, String email,
                               OffsetDateTime desde, OffsetDateTime hasta,
                               int page, int size) {

    public InvitacionFiltro {
        int resolvedSize = size <= 0 ? 20 : Math.min(size, 100);
        page = Math.max(page, 0);
        size = resolvedSize;
    }
}
