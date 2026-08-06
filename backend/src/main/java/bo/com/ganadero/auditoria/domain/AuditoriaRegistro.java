package bo.com.ganadero.auditoria.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditoriaRegistro(
        UUID id,
        UUID empresaId,
        UUID usuarioId,
        String accion,
        String modulo,
        String entidad,
        UUID entidadId,
        String correlationId,
        String resultado,
        Map<String, Object> datos,
        Map<String, Object> datosAnteriores,
        Map<String, Object> datosNuevos,
        String dispositivo,
        String ip,
        String userAgent,
        Instant createdAt) {
}
