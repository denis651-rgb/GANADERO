package bo.com.ganadero.auditoria.infrastructure;

import bo.com.ganadero.auditoria.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcAuditoriaRepository implements AuditoriaRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public JdbcAuditoriaRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(AuditoriaRegistro r) {
        jdbc.sql("""
                insert into auditoria_registro(id,usuario_id,accion,modulo,entidad,entidad_id,
                    correlation_id,resultado,datos,datos_anteriores,datos_nuevos,dispositivo,ip,user_agent,created_at)
                values(:id,:user,:accion,:modulo,:entidad,:entidadId,:corr,:resultado,
                    :datos,:antes,:nuevo,:dispositivo,:ip,:ua,:created)""")
                .param("id", r.id().toString())
                .param("user", r.usuarioId() == null ? null : r.usuarioId().toString())
                .param("accion", r.accion())
                .param("modulo", r.modulo())
                .param("entidad", r.entidad())
                .param("entidadId", r.entidadId() == null ? null : r.entidadId().toString())
                .param("corr", r.correlationId())
                .param("resultado", r.resultado())
                .param("datos", json(r.datos() == null ? Map.of() : r.datos()))
                .param("antes", json(r.datosAnteriores() == null ? Map.of() : r.datosAnteriores()))
                .param("nuevo", json(r.datosNuevos() == null ? Map.of() : r.datosNuevos()))
                .param("dispositivo", r.dispositivo())
                .param("ip", r.ip())
                .param("ua", r.userAgent())
                .param("created", r.createdAt().toString())
                .update();
    }

    @Override
    public AuditPage findAll(UUID empresa, AuditoriaFilter f) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (f.usuarioId() != null) { where.append(" and a.usuario_id=:user"); params.put("user", f.usuarioId().toString()); }
        if (f.modulo() != null && !f.modulo().isBlank()) { where.append(" and upper(a.modulo)=upper(:modulo)"); params.put("modulo", f.modulo()); }
        if (f.accion() != null && !f.accion().isBlank()) { where.append(" and upper(a.accion)=upper(:accion)"); params.put("accion", f.accion()); }
        if (f.entidad() != null && !f.entidad().isBlank()) { where.append(" and upper(a.entidad)=upper(:entidad)"); params.put("entidad", f.entidad()); }
        if (f.correlationId() != null && !f.correlationId().isBlank()) {
            where.append(" and a.correlation_id=:corr"); params.put("corr", f.correlationId());
        }
        // f.propiedadId() ya no aplica: la app maneja una sola finca local, no hay multi-propiedad que filtrar.
        if (f.desde() != null) { where.append(" and a.created_at>=:desde"); params.put("desde", f.desde().toString()); }
        if (f.hasta() != null) { where.append(" and a.created_at<:hasta"); params.put("hasta", f.hasta().toString()); }
        long total = jdbc.sql("select count(*) from auditoria_registro a" + where).params(params).query(Long.class).single();
        params.put("limit", f.size());
        params.put("offset", (long) f.page() * f.size());
        List<AuditoriaRegistro> values = jdbc.sql("select a.* from auditoria_registro a" + where
                        + " order by a.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return AuditPage.of(values, f.page(), f.size(), total);
    }

    @Override
    public List<AuditoriaRegistro> findLast(UUID empresa, UUID entidadId, String modulo, String entidad, int limit) {
        return jdbc.sql("""
                select a.* from auditoria_registro a
                where a.entidad_id=:entidadId
                  and (a.modulo=:modulo or a.entidad=:entidadTipo)
                order by a.created_at desc limit :limit""")
                .param("entidadId", entidadId == null ? null : entidadId.toString())
                .param("modulo", modulo).param("entidadTipo", entidad)
                .param("limit", limit)
                .query(this::map).list();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private AuditoriaRegistro map(ResultSet rs, int rowNum) throws SQLException {
        return new AuditoriaRegistro(
                UUID.fromString(rs.getString("id")),
                null,
                uuidOrNull(rs, "usuario_id"),
                rs.getString("accion"),
                rs.getString("modulo"),
                rs.getString("entidad"),
                uuidOrNull(rs, "entidad_id"),
                rs.getString("correlation_id"),
                rs.getString("resultado"),
                readJson(rs, "datos"),
                readJson(rs, "datos_anteriores"),
                readJson(rs, "datos_nuevos"),
                rs.getString("dispositivo"),
                rs.getString("ip"),
                rs.getString("user_agent"),
                java.time.Instant.parse(rs.getString("created_at")));
    }

    private UUID uuidOrNull(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return raw == null ? null : UUID.fromString(raw);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null || raw.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(raw, Map.class);
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
