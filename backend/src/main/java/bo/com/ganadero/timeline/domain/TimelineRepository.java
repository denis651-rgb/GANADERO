package bo.com.ganadero.timeline.domain;

import java.util.UUID;

/**
 * Repositorio de la línea de tiempo del animal (Bloque 23).
 */
public interface TimelineRepository {

    void insert(EventoTimelineAnimal evento);

    EventoTimelinePage findByAnimal(UUID animalId, UUID empresaId, EventoTimelineFilter filtro);
}
