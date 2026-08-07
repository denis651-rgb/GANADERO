package bo.com.ganadero.timeline.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Filtros de consulta de la línea de tiempo del animal (Bloque 29).
 *
 * @param tipo      Filtro por tipo estándar de evento.
 * @param modulo    Filtro por módulo de origen.
 * @param desde     Eventos desde esta fecha técnica inclusive.
 * @param hasta     Eventos hasta esta fecha técnica exclusiva.
 * @param usuarioId Filtro por usuario que registró el evento.
 * @param page      Página a consultar (desde cero).
 * @param size      Tamaño de página.
 */
public record EventoTimelineFilter(String tipo, String modulo, Instant desde, Instant hasta,
                                   UUID usuarioId, int page, int size) {
}
