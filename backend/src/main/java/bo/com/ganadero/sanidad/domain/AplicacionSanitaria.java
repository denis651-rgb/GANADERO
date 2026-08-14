package bo.com.ganadero.sanidad.domain; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record AplicacionSanitaria(UUID id,UUID empresaId,UUID jornadaId,UUID planItemId,UUID animalId,UUID productoId,
 UUID loteProductoId,BigDecimal dosis,String unidadDosis,String viaAdministracion,LocalDate fechaAplicacion,LocalDate proximaAplicacion,
 LocalDate retiroCarneHasta,LocalDate retiroLecheHasta,UUID aplicadoPor,String resultado,String observaciones,String idempotencyKey,String estado,long version) {}
