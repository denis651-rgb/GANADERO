package bo.com.ganadero.reproduccion.domain;
import java.time.LocalDate; import java.util.UUID;
public record Aborto(UUID id,UUID empresaId,UUID animalId,UUID gestacionId,UUID servicioId,LocalDate fechaEvento,
 Integer edadGestacionalEstimada,String causa,String diagnostico,UUID veterinarioId,String observaciones,UUID propiedadId,
 UUID potreroId,UUID loteId,UUID clienteUuid,String idempotencyKey,EstadoRegistroReproduccion estado,String codigoAnimal,
 String nombreAnimal,String potreroNombre,String propiedadNombre,long version) {}
