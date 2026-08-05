package bo.com.ganadero.auditoria.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditoriaFilter(
        UUID usuarioId,
        String modulo,
        String accion,
        String entidad,
        LocalDateTime desde,
        LocalDateTime hasta,
        int page,
        int size) {
}
