package bo.com.ganadero.sanidad.domain; import java.time.Instant; import java.util.UUID;
public record Enfermedad(UUID id,UUID empresaId,String codigo,String nombre,String descripcion,boolean esNotificable,boolean activo,Instant createdAt,Instant updatedAt) {}
