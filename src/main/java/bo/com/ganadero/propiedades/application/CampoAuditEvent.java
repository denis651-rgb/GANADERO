package bo.com.ganadero.propiedades.application;
import java.time.Instant; import java.util.UUID;
public record CampoAuditEvent(UUID empresaId,UUID usuarioId,String accion,String entidad,UUID entidadId,Instant occurredAt) {}
