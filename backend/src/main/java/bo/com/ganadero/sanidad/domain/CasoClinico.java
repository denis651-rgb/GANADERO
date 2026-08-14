package bo.com.ganadero.sanidad.domain;import java.time.Instant;import java.util.UUID;
public record CasoClinico(UUID id,UUID empresaId,UUID animalId,Instant fechaInicio,String sintomas,UUID enfermedadId,String diagnosticoTexto,SeveridadCaso severidad,EstadoCasoClinico estado,UUID veterinarioId,Instant fechaCierre,String resultado,String observaciones,long version){}
