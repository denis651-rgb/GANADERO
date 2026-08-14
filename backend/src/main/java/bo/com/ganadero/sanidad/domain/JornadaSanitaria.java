package bo.com.ganadero.sanidad.domain; import java.time.LocalDate; import java.util.UUID;
public record JornadaSanitaria(UUID id,UUID empresaId,TipoActividadSanitaria tipoJornada,LocalDate fechaInicio,LocalDate fechaFin,
 UUID propiedadId,UUID potreroId,UUID loteGanaderoId,UUID responsableId,UUID veterinarioId,EstadoJornada estado,
 String observaciones,UUID operationId,long version) {}
