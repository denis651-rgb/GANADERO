package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Agrupa los {@link EventoCalendarioSanitario} de varios animales que comparten actividad, fecha
 * exacta, propiedad, potrero y lote — la unidad que en el futuro se sincronizará como un solo
 * evento externo (Google Calendar), en vez de uno por animal. {@code loteGanaderoId} es opcional
 * porque no todo animal pertenece a un lote.
 */
public record OcurrenciaCalendarioSanitario(
        UUID id, UUID planItemId, Instant fechaPrevista, UUID propiedadId, UUID potreroId, UUID loteGanaderoId,
        String ocurrenciaClave, Instant createdAt, long version) {
}
