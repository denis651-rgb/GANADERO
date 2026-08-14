package bo.com.ganadero.sanidad.application; import java.time.Instant; import java.util.UUID;
public record SanidadAuditEvent(UUID empresaId,UUID usuarioId,String accion,String entidad,UUID entidadId,Instant occurredAt) {}
