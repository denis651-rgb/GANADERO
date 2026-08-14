package bo.com.ganadero.reproduccion.domain;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record Destete(UUID id,UUID empresaId,UUID animalCriaId,UUID madreId,LocalDate fechaDestete,BigDecimal pesoDesteteKg,
 TipoDestete tipoDestete,String motivo,UUID responsableId,String observaciones,UUID propiedadId,UUID potreroId,UUID loteId,
 UUID clienteUuid,String idempotencyKey,EstadoRegistroReproduccion estado,String codigoAnimal,String nombreAnimal,
 String potreroNombre,String propiedadNombre,long version) {}
