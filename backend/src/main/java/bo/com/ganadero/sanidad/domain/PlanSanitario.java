package bo.com.ganadero.sanidad.domain; import java.time.*; import java.util.UUID;
public record PlanSanitario(UUID id,UUID empresaId,String nombre,String descripcion,LocalDate fechaInicio,LocalDate fechaFin,
 EstadoPlanSanitario estado,Instant createdAt,Instant updatedAt,long version) {}
