package bo.com.ganadero.auditoria.infrastructure;

import bo.com.ganadero.auditoria.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
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
                insert into auditoria.registros(id,empresa_id,usuario_id,accion,modulo,entidad,entidad_id,
                    correlation_id,resultado,datos,datos_anteriores,datos_nuevos,dispositivo,ip,user_agent,created_at)
                values(:id,:e,:user,:accion,:modulo,:entidad,:entidadId,:corr,:resultado,
                    :datos::jsonb,:antes::jsonb,:nuevo::jsonb,:dispositivo,:ip,:ua,:created)""")
                .param("id", r.id())
                .param("e", r.empresaId())
                .param("user", r.usuarioId())
                .param("accion", r.accion())
                .param("modulo", r.modulo())
                .param("entidad", r.entidad())
                .param("entidadId", r.entidadId())
                .param("corr", r.correlationId())
                .param("resultado", r.resultado())
                .param("datos", json(r.datos() == null ? Map.of() : r.datos()))
                .param("antes", json(r.datosAnteriores() == null ? Map.of() : r.datosAnteriores()))
                .param("nuevo", json(r.datosNuevos() == null ? Map.of() : r.datosNuevos()))
                .param("dispositivo", r.dispositivo())
                .param("ip", r.ip())
                .param("ua", r.userAgent())
                .param("created", java.sql.Timestamp.from(r.createdAt()))
                .update();
    }

    @Override
    public AuditPage findAll(UUID empresa, AuditoriaFilter f) {
        StringBuilder where = new StringBuilder(" where a.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (f.usuarioId() != null) { where.append(" and a.usuario_id=:user"); params.put("user", f.usuarioId()); }
        if (f.modulo() != null && !f.modulo().isBlank()) { where.append(" and upper(a.modulo)=upper(:modulo)"); params.put("modulo", f.modulo()); }
        if (f.accion() != null && !f.accion().isBlank()) { where.append(" and upper(a.accion)=upper(:accion)"); params.put("accion", f.accion()); }
        if (f.entidad() != null && !f.entidad().isBlank()) { where.append(" and upper(a.entidad)=upper(:entidad)"); params.put("entidad", f.entidad()); }
        if (f.correlationId() != null && !f.correlationId().isBlank()) {
            where.append(" and a.correlation_id=:corr"); params.put("corr", f.correlationId());
        }
        if (f.propiedadId() != null) {
            where.append("""
                     and a.entidad_id in (
                         select pr.id from core.propiedades pr where pr.id=:propiedad and pr.empresa_id=:e
                         union all select pot.id from campo.potreros pot where pot.propiedad_id=:propiedad and pot.empresa_id=:e
                         union all select sec.id from campo.sectores sec where sec.propiedad_id=:propiedad and sec.empresa_id=:e
                         union all select an.id from ganado.animales an where an.propiedad_actual_id=:propiedad and an.empresa_id=:e
                         union all select idt.id from ganado.identificadores_animal idt
                             join ganado.animales an on an.id=idt.animal_id
                             where an.propiedad_actual_id=:propiedad and idt.empresa_id=:e
                         union all select pa.id from ganado.parentescos pa
                             join ganado.animales an on an.id=pa.animal_id
                             where an.propiedad_actual_id=:propiedad and pa.empresa_id=:e
                         union all select lo.id from ganado.lotes_ganaderos lo where lo.propiedad_id=:propiedad and lo.empresa_id=:e
                         union all select mv.id from ganado.movimientos mv where mv.empresa_id=:e and (
                             mv.origen_propiedad_id=:propiedad or mv.destino_propiedad_id=:propiedad
                             or mv.origen_potrero_id in (select pot.id from campo.potreros pot where pot.propiedad_id=:propiedad)
                             or mv.destino_potrero_id in (select pot.id from campo.potreros pot where pot.propiedad_id=:propiedad)
                             or mv.origen_lote_id in (select lo.id from ganado.lotes_ganaderos lo where lo.propiedad_id=:propiedad)
                             or mv.destino_lote_id in (select lo.id from ganado.lotes_ganaderos lo where lo.propiedad_id=:propiedad))
                         union all select pe.id from produccion.pesajes pe where pe.propiedad_id=:propiedad and pe.empresa_id=:e
                     )""");
            params.put("propiedad", f.propiedadId());
        }
        if (f.desde() != null) { where.append(" and a.created_at>=:desde"); params.put("desde", f.desde()); }
        if (f.hasta() != null) { where.append(" and a.created_at<:hasta"); params.put("hasta", f.hasta()); }
        long total = jdbc.sql("select count(*) from auditoria.registros a" + where).params(params).query(Long.class).single();
        params.put("limit", f.size());
        params.put("offset", (long) f.page() * f.size());
        List<AuditoriaRegistro> values = jdbc.sql("select a.* from auditoria.registros a" + where
                        + " order by a.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return AuditPage.of(values, f.page(), f.size(), total);
    }

    @Override
    public List<AuditoriaRegistro> findLast(UUID empresa, UUID entidadId, String modulo, String entidad, int limit) {
        return jdbc.sql("""
                select a.* from auditoria.registros a
                where a.empresa_id=:e and a.entidad_id=:entidad
                  and (a.modulo=:modulo or a.entidad=:entidad)
                order by a.created_at desc limit :limit""")
                .param("e", empresa).param("entidad", entidadId)
                .param("modulo", modulo).param("entidad", entidad)
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
                rs.getObject("id", UUID.class),
                rs.getObject("empresa_id", UUID.class),
                rs.getObject("usuario_id", UUID.class),
                rs.getString("accion"),
                rs.getString("modulo"),
                rs.getString("entidad"),
                rs.getObject("entidad_id", UUID.class),
                rs.getString("correlation_id"),
                rs.getString("resultado"),
                readJson(rs, "datos"),
                readJson(rs, "datos_anteriores"),
                readJson(rs, "datos_nuevos"),
                rs.getString("dispositivo"),
                rs.getString("ip"),
                rs.getString("user_agent"),
                rs.getTimestamp("created_at").toInstant());
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
