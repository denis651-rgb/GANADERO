package bo.com.ganadero.timeline.infrastructure;

import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.timeline.domain.EventoTimelineAnimal;
import bo.com.ganadero.timeline.domain.EventoTimelineFilter;
import bo.com.ganadero.timeline.domain.EventoTimelinePage;
import bo.com.ganadero.timeline.domain.TimelineRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
class JdbcTimelineRepository implements TimelineRepository {

    private static final String SELECT = """
            select e.id,e.animal_id,e.tipo,e.titulo,e.descripcion,
                e.fecha_tecnica,e.fecha_evento,e.usuario_id,e.dispositivo,e.modulo_origen,
                e.registro_origen,e.metadata,e.idempotency_key,e.created_at
            from evento_animal e""";

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    JdbcTimelineRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(EventoTimelineAnimal evento) {
        Map<String, Object> metadata = new HashMap<>(evento.metadata() == null ? Map.of() : evento.metadata());
        String json;
        try {
            json = objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            json = "{}";
        }
        try {
            jdbc.sql("""
                    insert into evento_animal(id,animal_id,tipo,titulo,descripcion,
                        fecha_evento,fecha_tecnica,usuario_id,modulo_origen,registro_origen,
                        metadata,idempotency_key,created_by,created_at)
                    values(:id,:animal,:tipo,:titulo,:descripcion,
                        :fechaEvento,:fechaTecnica,:usuario,:modulo,:registro,
                        :metadata,:idempotency,:usuario,:createdAt)""")
                    .param("id", evento.id())
                    .param("animal", evento.animalId())
                    .param("tipo", evento.tipo().name())
                    .param("titulo", evento.titulo())
                    .param("descripcion", evento.descripcion())
                    .param("fechaEvento", evento.fechaEvento().toString())
                    .param("fechaTecnica", (evento.fechaTecnica() == null ? evento.fechaEvento() : evento.fechaTecnica()).toString())
                    .param("usuario", evento.usuarioId())
                    .param("modulo", evento.moduloOrigen())
                    .param("registro", evento.registroOrigenId())
                    .param("metadata", json)
                    .param("idempotency", evento.idempotencyKey())
                    .param("createdAt", java.time.Instant.now().toString())
                    .update();
        } catch (DuplicateKeyException ex) {
            if (evento.idempotencyKey() == null) {
                throw ex;
            }
        }
    }

    @Override
    public EventoTimelinePage findByAnimal(UUID animalId, UUID empresaId, EventoTimelineFilter filtro) {
        StringBuilder where = new StringBuilder(" where e.animal_id=:animal");
        Map<String, Object> params = new HashMap<>();
        params.put("animal", animalId);
        if (filtro.tipo() != null && !filtro.tipo().isBlank()) {
            where.append(" and e.tipo=:tipo");
            params.put("tipo", filtro.tipo());
        }
        if (filtro.modulo() != null && !filtro.modulo().isBlank()) {
            where.append(" and upper(e.modulo_origen)=upper(:modulo)");
            params.put("modulo", filtro.modulo());
        }
        if (filtro.desde() != null) {
            where.append(" and e.fecha_tecnica >= :desde");
            params.put("desde", filtro.desde().toString());
        }
        if (filtro.hasta() != null) {
            where.append(" and e.fecha_tecnica < :hasta");
            params.put("hasta", filtro.hasta().toString());
        }
        if (filtro.usuarioId() != null) {
            where.append(" and e.usuario_id=:usuario");
            params.put("usuario", filtro.usuarioId());
        }
        long total = jdbc.sql("select count(*) from evento_animal e" + where)
                .params(params).query(Long.class).single();
        params.put("limit", filtro.size());
        params.put("offset", (long) filtro.page() * filtro.size());
        var values = jdbc.sql(SELECT + where + " order by e.fecha_evento desc, e.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return EventoTimelinePage.of(values, filtro.page(), filtro.size(), total);
    }

    private EventoTimelineAnimal map(ResultSet r, int rowNum) throws SQLException {
        String tipo = r.getString("tipo");
        return new EventoTimelineAnimal(
                Rows.uuid(r, "id"),
                null,
                Rows.uuid(r, "animal_id"),
                bo.com.ganadero.timeline.domain.TipoEventoAnimal.valueOf(tipo),
                r.getString("titulo"),
                r.getString("descripcion"),
                Rows.instant(r, "fecha_tecnica"),
                Rows.instant(r, "fecha_evento"),
                Rows.uuid(r, "usuario_id"),
                null,
                null,
                r.getString("modulo_origen"),
                Rows.uuid(r, "registro_origen"),
                readMetadata(r.getString("metadata")),
                r.getString("idempotency_key"),
                Rows.instant(r, "created_at"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMetadata(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, Object> value = objectMapper.readValue(json, Map.class);
            return value == null ? Map.of() : value;
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
