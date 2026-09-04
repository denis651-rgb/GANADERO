package bo.com.ganadero.configuracion.application;
import java.time.Instant; import java.util.UUID;
public record ConfiguracionAuditEvent(UUID empresaId, UUID usuarioId, Instant occurredAt) {}
