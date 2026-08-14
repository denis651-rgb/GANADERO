package bo.com.ganadero.sanidad.domain;import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
public record AplicacionTratamiento(UUID id,UUID empresaId,UUID tratamientoDetalleId,Instant fechaProgramada,Instant fechaAplicada,BigDecimal dosisProgramada,BigDecimal dosisAplicada,UUID aplicadoPor,EstadoAplicacionTratamiento estado,String observaciones,long version){}
