package bo.com.ganadero.seguridad.application;

import java.time.Instant;
import java.util.UUID;

public record SeguridadAuditEvent(UUID empresaId, UUID usuarioId, String accion,
                                  String entidadTipo, UUID entidadId, Instant fecha) {}
