package bo.com.ganadero.timeline.application;

import bo.com.ganadero.timeline.domain.TipoEventoAnimal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Comando de publicación de un evento en la línea de tiempo del animal.
 *
 * <p>Los valores {@code titulo} y {@code moduloOrigen} se completan con los
 * valores por defecto del {@link TipoEventoAnimal} si no se indican.</p>
 *
 * @param idempotencyKey Si es null y hay {@code registroOrigenId}, se deriva
 *                       {@code modulo|registroOrigenId|tipo|animalId} (Bloque 27).
 */
public record RegistrarEventoTimeline(
        UUID empresaId,
        UUID animalId,
        TipoEventoAnimal tipo,
        String titulo,
        String descripcion,
        String moduloOrigen,
        UUID registroOrigenId,
        Map<String, Object> metadata,
        UUID usuarioId,
        Instant fechaTecnica,
        String idempotencyKey) {
}
