package bo.com.ganadero.auditoria.api;

import bo.com.ganadero.auditoria.domain.AuditoriaRegistro;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditoriaResponse(
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

    public static AuditoriaResponse from(AuditoriaRegistro r) {
        return new AuditoriaResponse(
                r.id(), r.empresaId(), r.usuarioId(), r.accion(), r.modulo(), r.entidad(), r.entidadId(),
                r.correlationId(), r.resultado(), r.datos(), r.datosAnteriores(), r.datosNuevos(),
                r.dispositivo(), r.ip(), r.userAgent(), r.createdAt());
    }
}
