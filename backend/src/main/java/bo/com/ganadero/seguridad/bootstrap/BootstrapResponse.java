package bo.com.ganadero.seguridad.bootstrap;
import java.util.UUID;
public record BootstrapResponse(UUID empresaId, UUID usuarioId, UUID miembroId, UUID propiedadId, String estado) {}
