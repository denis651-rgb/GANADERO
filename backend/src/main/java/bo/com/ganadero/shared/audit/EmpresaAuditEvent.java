package bo.com.ganadero.shared.audit;

import java.time.Instant;
import java.util.UUID;

public record EmpresaAuditEvent(
        UUID empresaId,
        UUID usuarioId,
        String entidadTipo,
        UUID entidadId,
        Instant fecha) {
}
