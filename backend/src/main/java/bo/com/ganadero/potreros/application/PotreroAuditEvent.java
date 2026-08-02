package bo.com.ganadero.potreros.application; import java.time.Instant; import java.util.UUID;
public record PotreroAuditEvent(UUID empresaId,UUID usuarioId,String accion,String entidad,UUID entidadId,Instant occurredAt) {}
