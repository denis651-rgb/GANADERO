package bo.com.ganadero.timeline.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Modelo estándar de evento de la línea de tiempo del animal (Bloque 24).
 *
 * @param id                Identificador del evento.
 * @param empresaId         Empresa propietaria del evento.
 * @param animalId          Animal al que pertenece el evento.
 * @param tipo              Tipo estándar del evento (Bloque 25).
 * @param titulo            Título legible del evento.
 * @param descripcion       Detalle textual opcional.
 * @param fechaTecnica      Fecha técnica del registro (fecha técnica del dato).
 * @param fechaEvento       Fecha del evento de negocio.
 * @param usuarioId         Usuario que registró el evento.
 * @param usuarioNombre     Nombre legible del usuario (poblado al consultar).
 * @param dispositivoId     Dispositivo de origen (sincronización) si existe.
 * @param moduloOrigen      Módulo que originó el evento.
 * @param registroOrigenId  Identificador del registro de negocio que originó el evento.
 * @param metadata          Datos adicionales específicos del tipo de evento.
 * @param idempotencyKey    Clave de idempotencia (Bloque 27).
 * @param createdAt         Fecha de creación en base de datos.
 */
public record EventoTimelineAnimal(
        UUID id,
        UUID empresaId,
        UUID animalId,
        TipoEventoAnimal tipo,
        String titulo,
        String descripcion,
        Instant fechaTecnica,
        Instant fechaEvento,
        UUID usuarioId,
        String usuarioNombre,
        UUID dispositivoId,
        String moduloOrigen,
        UUID registroOrigenId,
        Map<String, Object> metadata,
        String idempotencyKey,
        Instant createdAt) {
}
