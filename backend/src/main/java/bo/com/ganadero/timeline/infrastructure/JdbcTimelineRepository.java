package bo.com.ganadero.timeline.infrastructure;

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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
class JdbcTimelineRepository implements TimelineRepository {

    private static final String SELECT = """
            select e.id,e.empresa_id,e.animal_id,e.tipo,e.titulo,e.descripcion,
                e.fecha_tecnica,e.fecha_evento,e.usuario_id,e.dispositivo_id,e.modulo_origen,
                e.registro_origen,e.metadata,e.idempotency_key,e.created_at,
                nullif(trim(concat(coalesce(u.nombres,''),' ',coalesce(u.apellidos,''))),'') as usuario_nombre
            from ganado.eventos_animal e
            left join seguridad.perfiles_usuario u on u.id = e.usuario_id""";

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    JdbcTimelineRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(EventoTimelineAnimal evento) {
        UUID dispositivoId = evento.dispositivoId();
        Map<String, Object> metadata = new HashMap<>(evento.metadata() == null ? Map.of() : evento.metadata());
        String codigoDispositivo = dispositivoActual();
        if (dispositivoId == null && codigoDispositivo != null && !codigoDispositivo.isBlank()) {
            UUID id = dispositivoId(evento.empresaId(), codigoDispositivo);
            if (id != null) {
                dispositivoId = id;
                metadata.putIfAbsent("origenSync", true);
            }
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(metadata);
        } catch (Exception ex) {
            json = "{}";
        }
        try {
            jdbc.sql("""
                    insert into ganado.eventos_animal(id,empresa_id,animal_id,tipo,titulo,descripcion,
                        fecha_evento,fecha_tecnica,usuario_id,dispositivo_id,modulo_origen,registro_origen,
                        metadata,idempotency_key,registrado_por,created_by,created_at)
                    values(:id,:e,:animal,:tipo,:titulo,:descripcion,
                        :fechaEvento,:fechaTecnica,:usuario,:dispositivo,:modulo,:registro,
                        :metadata::jsonb,:idempotency,:usuario,:usuario,now())""")
                    .param("id", evento.id())
                    .param("e", evento.empresaId())
                    .param("animal", evento.animalId())
                    .param("tipo", evento.tipo().name())
                    .param("titulo", evento.titulo())
                    .param("descripcion", evento.descripcion())
                    .param("fechaEvento", Timestamp.from(evento.fechaEvento()))
                    .param("fechaTecnica", evento.fechaTecnica() == null ? Timestamp.from(evento.fechaEvento()) : Timestamp.from(evento.fechaTecnica()))
                    .param("usuario", evento.usuarioId())
                    .param("dispositivo", dispositivoId)
                    .param("modulo", evento.moduloOrigen())
                    .param("registro", evento.registroOrigenId())
                    .param("metadata", json)
                    .param("idempotency", evento.idempotencyKey())
                    .update();
        } catch (DuplicateKeyException ex) {
            if (evento.idempotencyKey() == null) {
                throw ex;
            }
        }
    }

    @Override
    public EventoTimelinePage findByAnimal(UUID animalId, UUID empresaId, EventoTimelineFilter filtro) {
        StringBuilder where = new StringBuilder(" where e.animal_id=:animal and e.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("animal", animalId);
        params.put("e", empresaId);
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
            params.put("desde", Timestamp.from(filtro.desde()));
        }
        if (filtro.hasta() != null) {
            where.append(" and e.fecha_tecnica < :hasta");
            params.put("hasta", Timestamp.from(filtro.hasta()));
        }
        if (filtro.usuarioId() != null) {
            where.append(" and e.usuario_id=:usuario");
            params.put("usuario", filtro.usuarioId());
        }
        long total = jdbc.sql("select count(*) from ganado.eventos_animal e" + where)
                .params(params).query(Long.class).single();
        params.put("limit", filtro.size());
        params.put("offset", (long) filtro.page() * filtro.size());
        var values = jdbc.sql(SELECT + where + " order by e.fecha_evento desc, e.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return EventoTimelinePage.of(values, filtro.page(), filtro.size(), total);
    }

    private String dispositivoActual() {
        return jdbc.sql("select current_setting('sync.dispositivo', true)")
                .query(String.class).single();
    }

    private UUID dispositivoId(UUID empresa, String codigo) {
        return jdbc.sql("select id from sync.dispositivos where empresa_id=:e and codigo_dispositivo=:codigo")
                .param("e", empresa).param("codigo", codigo).query(UUID.class).optional().orElse(null);
    }

    private EventoTimelineAnimal map(ResultSet r, int rowNum) throws SQLException {
        String tipo = r.getString("tipo");
        Timestamp fechaTecnica = r.getTimestamp("fecha_tecnica");
        return new EventoTimelineAnimal(
                r.getObject("id", UUID.class),
                r.getObject("empresa_id", UUID.class),
                r.getObject("animal_id", UUID.class),
                bo.com.ganadero.timeline.domain.TipoEventoAnimal.valueOf(tipo),
                r.getString("titulo"),
                r.getString("descripcion"),
                fechaTecnica == null ? null : fechaTecnica.toInstant(),
                r.getTimestamp("fecha_evento").toInstant(),
                r.getObject("usuario_id", UUID.class),
                r.getString("usuario_nombre"),
                r.getObject("dispositivo_id", UUID.class),
                r.getString("modulo_origen"),
                r.getObject("registro_origen", UUID.class),
                readMetadata(r.getString("metadata")),
                r.getString("idempotency_key"),
                r.getTimestamp("created_at").toInstant());
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
