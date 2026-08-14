package bo.com.ganadero.sanidad.domain;import java.time.Instant;import java.util.UUID;
public record Tratamiento(UUID id,UUID empresaId,UUID casoClinicoId,UUID animalId,Instant fechaInicio,Instant fechaFinEstimada,Instant fechaFinReal,String diagnostico,UUID veterinarioId,EstadoTratamiento estado,String observaciones,long version){}
