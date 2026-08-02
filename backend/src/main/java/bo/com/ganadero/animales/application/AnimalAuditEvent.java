package bo.com.ganadero.animales.application; import java.time.Instant; import java.util.UUID;
public record AnimalAuditEvent(UUID empresaId,UUID usuarioId,String accion,String entidad,UUID entidadId,Instant occurredAt) {}
