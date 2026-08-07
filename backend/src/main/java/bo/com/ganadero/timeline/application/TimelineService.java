package bo.com.ganadero.timeline.application;

import bo.com.ganadero.timeline.api.TimelinePageResponse;
import bo.com.ganadero.timeline.domain.EventoTimelineAnimal;
import bo.com.ganadero.timeline.domain.EventoTimelineFilter;
import bo.com.ganadero.timeline.domain.TimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio central de la línea de tiempo del animal (Bloque 23).
 */
@Service
public class TimelineService implements TimelineEventPublisher {

    private final TimelineRepository timelineRepository;

    public TimelineService(TimelineRepository timelineRepository) {
        this.timelineRepository = timelineRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void publish(RegistrarEventoTimeline evento) {
        timelineRepository.insert(toEntity(evento));
    }

    @Transactional(readOnly = true)
    public TimelinePageResponse timeline(UUID empresaId, UUID animalId, EventoTimelineFilter filtro) {
        return TimelinePageResponse.from(timelineRepository.findByAnimal(animalId, empresaId, filtro));
    }

    private EventoTimelineAnimal toEntity(RegistrarEventoTimeline evento) {
        String modulo = evento.moduloOrigen() != null ? evento.moduloOrigen() : evento.tipo().modulo();
        String titulo = evento.titulo() != null ? evento.titulo() : evento.tipo().titulo();
        Instant fecha = evento.fechaTecnica() != null ? evento.fechaTecnica() : Instant.now();
        String key = evento.idempotencyKey();
        if (key == null && evento.registroOrigenId() != null) {
            key = modulo + "|" + evento.registroOrigenId() + "|" + evento.tipo().name() + "|" + evento.animalId();
        }
        return new EventoTimelineAnimal(
                UUID.randomUUID(),
                evento.empresaId(),
                evento.animalId(),
                evento.tipo(),
                titulo,
                evento.descripcion(),
                fecha,
                fecha,
                evento.usuarioId(),
                null,
                null,
                modulo,
                evento.registroOrigenId(),
                evento.metadata() == null ? Map.of() : evento.metadata(),
                key,
                Instant.now());
    }
}
