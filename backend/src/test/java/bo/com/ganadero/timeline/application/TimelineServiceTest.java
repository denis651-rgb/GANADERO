package bo.com.ganadero.timeline.application;

import bo.com.ganadero.timeline.api.TimelinePageResponse;
import bo.com.ganadero.timeline.domain.EventoTimelineAnimal;
import bo.com.ganadero.timeline.domain.EventoTimelineFilter;
import bo.com.ganadero.timeline.domain.EventoTimelinePage;
import bo.com.ganadero.timeline.domain.TimelineRepository;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimelineServiceTest {
    private TimelineRepository repo;
    private final List<EventoTimelineAnimal> insertados = new ArrayList<>();
    private TimelineService service;
    private UUID company;
    private UUID animal;
    private UUID usuario;
    private UUID registro;

    @BeforeEach
    void setup() {
        insertados.clear();
        repo = mock(TimelineRepository.class);
        doAnswer(inv -> {
            insertados.add(inv.getArgument(0));
            return null;
        }).when(repo).insert(any(EventoTimelineAnimal.class));
        service = new TimelineService(repo);
        company = UUID.randomUUID();
        animal = UUID.randomUUID();
        usuario = UUID.randomUUID();
        registro = UUID.randomUUID();
    }

    @Test
    void publishPersisteEventoConValoresPorDefecto() {
        service.publish(new RegistrarEventoTimeline(company, animal, TipoEventoAnimal.LOTE_ASIGNADO, null,
                "Descripción", null, registro, null, usuario, Instant.parse("2026-01-01T00:00:00Z"), null));
        assertThat(insertados).hasSize(1);
        EventoTimelineAnimal ev = insertados.get(0);
        assertThat(ev.empresaId()).isEqualTo(company);
        assertThat(ev.animalId()).isEqualTo(animal);
        assertThat(ev.tipo()).isEqualTo(TipoEventoAnimal.LOTE_ASIGNADO);
        assertThat(ev.titulo()).isEqualTo(TipoEventoAnimal.LOTE_ASIGNADO.titulo());
        assertThat(ev.moduloOrigen()).isEqualTo(TipoEventoAnimal.LOTE_ASIGNADO.modulo());
        assertThat(ev.registroOrigenId()).isEqualTo(registro);
        assertThat(ev.metadata()).isEmpty();
        assertThat(ev.usuarioId()).isEqualTo(usuario);
        assertThat(ev.id()).isNotNull();
        assertThat(ev.fechaEvento()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(ev.fechaTecnica()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        verify(repo).insert(any(EventoTimelineAnimal.class));
    }

    @Test
    void publishDerivaClaveIdempotenciaCuandoNoSeIndica() {
        service.publish(new RegistrarEventoTimeline(company, animal, TipoEventoAnimal.PESAJE_REGISTRADO,
                null, null, null, registro, null, usuario, null, null));
        EventoTimelineAnimal ev = insertados.get(0);
        assertThat(ev.idempotencyKey())
                .isEqualTo("PESAJES|" + registro + "|PESAJE_REGISTRADO|" + animal);
    }

    @Test
    void publishPreservaClaveIdempotenciaExplicita() {
        service.publish(new RegistrarEventoTimeline(company, animal, TipoEventoAnimal.FOTO_AGREGADA,
                null, null, null, registro, null, usuario, null, "explicita-123"));
        assertThat(insertados.get(0).idempotencyKey()).isEqualTo("explicita-123");
    }

    @Test
    void publishSinRegistroOrigenNiClaveDejaClaveNula() {
        service.publish(new RegistrarEventoTimeline(company, animal, TipoEventoAnimal.FOTO_AGREGADA,
                null, null, null, null, null, usuario, null, null));
        assertThat(insertados.get(0).idempotencyKey()).isNull();
    }

    @Test
    void reintentoDelMismoEventoGeneraLaMismaClave() {
        RegistrarEventoTimeline evento = new RegistrarEventoTimeline(company, animal,
                TipoEventoAnimal.MOVIMIENTO_REGISTRADO, null, null, null, registro, null, usuario, null, null);
        service.publish(evento);
        service.publish(evento);
        assertThat(insertados).hasSize(2);
        assertThat(insertados.get(0).idempotencyKey()).isEqualTo(insertados.get(1).idempotencyKey());
        assertThat(insertados.get(0).registroOrigenId()).isEqualTo(insertados.get(1).registroOrigenId());
        assertThat(insertados.get(0).tipo()).isEqualTo(insertados.get(1).tipo());
    }

    @Test
    void publishCompletaTituloYModuloUsandoElTipo() {
        service.publish(new RegistrarEventoTimeline(company, animal, TipoEventoAnimal.CUARENTENA_INICIADA,
                null, null, "SINCRONIZACION", null, null, usuario, null, null));
        EventoTimelineAnimal ev = insertados.get(0);
        assertThat(ev.titulo()).isEqualTo(TipoEventoAnimal.CUARENTENA_INICIADA.titulo());
        assertThat(ev.moduloOrigen()).isEqualTo("SINCRONIZACION");
    }

    @Test
    void publishFechaTecnicaPorDefectoUsaAhora() {
        service.publish(new RegistrarEventoTimeline(company, animal, TipoEventoAnimal.ESTADO_CAMBIADO,
                null, null, null, null, null, usuario, null, null));
        EventoTimelineAnimal ev = insertados.get(0);
        assertThat(ev.fechaTecnica()).isNotNull();
        assertThat(ev.fechaEvento()).isNotNull();
    }

    @Test
    void timelineDelegaConsultaAlRepositorio() {
        EventoTimelineFilter filtro = new EventoTimelineFilter("MOVIMIENTO_REGISTRADO", "MOVIMIENTOS",
                null, null, usuario, 0, 20);
        EventoTimelineAnimal evento = new EventoTimelineAnimal(UUID.randomUUID(), company, animal,
                TipoEventoAnimal.MOVIMIENTO_REGISTRADO, "Movimiento registrado", null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                usuario, null, null, "MOVIMIENTOS", UUID.randomUUID(), java.util.Map.of(),
                "MOVIMIENTOS|" + registro + "|MOVIMIENTO_REGISTRADO|" + animal, Instant.now());
        when(repo.findByAnimal(animal, company, filtro))
                .thenReturn(EventoTimelinePage.of(List.of(evento), 0, 20, 1));
        TimelinePageResponse page = service.timeline(company, animal, filtro);
        verify(repo).findByAnimal(animal, company, filtro);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).idempotencyKey()).isNotNull();
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
