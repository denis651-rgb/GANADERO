package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcAlertaRepository implements AlertaRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcAlertaRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Alerta programar(Alerta a) {
        String sql = """
                INSERT INTO alerta(id, animal_id, tipo, titulo, mensaje, severidad, fecha_programada,
                                   fecha_vencimiento, origen_tipo, origen_id, estado, metadata, clave_idempotencia)
                VALUES(:id, :an, :t, :ti, :m, :s, :fp, :fv, :ot, :oi, :es, :md, :ci)
                ON CONFLICT(clave_idempotencia) DO UPDATE SET
                    titulo = excluded.titulo,
                    mensaje = excluded.mensaje,
                    severidad = excluded.severidad,
                    fecha_programada = excluded.fecha_programada,
                    fecha_vencimiento = excluded.fecha_vencimiento,
                    metadata = excluded.metadata,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                RETURNING id
                """;
        String id = jdbc.sql(sql)
                .param("id", a.id())
                .param("an", a.animalId())
                .param("t", a.tipo().name())
                .param("ti", a.titulo())
                .param("m", a.mensaje())
                .param("s", a.severidad().name())
                .param("fp", ts(a.fechaProgramada()))
                .param("fv", ts(a.fechaVencimiento()))
                .param("ot", a.origenTipo())
                .param("oi", a.origenId())
                .param("es", a.estado().name())
                .param("md", write(a.metadata()))
                .param("ci", a.claveIdempotencia())
                .query(String.class)
                .single();
        return buscar(UUID.fromString(id), a.empresaId()).orElseThrow();
    }

    @Override
    public Optional<Alerta> evolucionarOrigen(Alerta a, Collection<TipoAlerta> anteriores) {
        if (anteriores == null || anteriores.isEmpty()) {
            return Optional.empty();
        }
        String tipos = anteriores.stream()
                .map(t -> "'" + t.name() + "'")
                .collect(Collectors.joining(","));
        String sql = "UPDATE alerta\n"
                + "SET tipo = :t,\n"
                + "    titulo = :ti,\n"
                + "    mensaje = :m,\n"
                + "    severidad = :s,\n"
                + "    fecha_programada = :fp,\n"
                + "    fecha_vencimiento = :fv,\n"
                + "    metadata = :md,\n"
                + "    clave_idempotencia = :ci,\n"
                + "    estado = CASE WHEN tipo <> :t OR severidad <> :s THEN 'PENDIENTE' ELSE estado END,\n"
                + "    enviada_at = CASE WHEN tipo <> :t OR severidad <> :s THEN NULL ELSE enviada_at END,\n"
                + "    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')\n"
                + "WHERE origen_tipo = :ot\n"
                + "  AND origen_id = :oi\n"
                + "  AND tipo IN (" + tipos + ")\n"
                + "  AND estado IN ('PROGRAMADA', 'PENDIENTE', 'ENVIADA', 'ATENDIDA', 'ERROR')\n"
                + "RETURNING id";
        Optional<String> id = jdbc.sql(sql)
                .param("t", a.tipo().name())
                .param("ti", a.titulo())
                .param("m", a.mensaje())
                .param("s", a.severidad().name())
                .param("fp", ts(a.fechaProgramada()))
                .param("fv", ts(a.fechaVencimiento()))
                .param("md", write(a.metadata()))
                .param("ci", a.claveIdempotencia())
                .param("ot", a.origenTipo())
                .param("oi", a.origenId())
                .query(String.class)
                .optional();
        return id.map(UUID::fromString).flatMap(x -> buscar(x, a.empresaId()));
    }

    @Override
    public Optional<Alerta> buscar(UUID id, UUID empresaId) {
        return jdbc.sql("SELECT * FROM alerta WHERE id = :id")
                .param("id", id)
                .query(this::map)
                .optional();
    }

    @Override
    public List<Alerta> listar(UUID empresaId, EstadoAlerta estado, TipoAlerta tipo, UUID animalId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM alerta WHERE 1=1");
        if (estado != null) {
            sql.append(" AND estado = :s");
        }
        if (tipo != null) {
            sql.append(" AND tipo = :t");
        }
        if (animalId != null) {
            sql.append(" AND animal_id = :a");
        }
        sql.append(" ORDER BY fecha_programada DESC");

        var query = jdbc.sql(sql.toString());
        if (estado != null) query = query.param("s", estado.name());
        if (tipo != null) query = query.param("t", tipo.name());
        if (animalId != null) query = query.param("a", animalId);
        return query.query(this::map).list();
    }

    @Override
    public void resolverOrigen(UUID empresaId, String origenTipo, UUID origenId, UUID actor) {
        String sql = """
                UPDATE alerta
                SET estado = 'RESUELTA',
                    resuelta_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                    resuelta_por = :a,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE origen_tipo = :o
                  AND origen_id = :id
                  AND estado IN ('PROGRAMADA', 'PENDIENTE', 'ENVIADA', 'ATENDIDA', 'ERROR')
                """;
        jdbc.sql(sql)
                .param("a", actor)
                .param("o", origenTipo)
                .param("id", origenId)
                .update();
    }

    @Override
    public void cancelarOrigen(UUID empresaId, String origenTipo, UUID origenId, String motivo) {
        String sql = """
                UPDATE alerta
                SET estado = 'CANCELADA',
                    cancelada_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'),
                    motivo_cancelacion = :m,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE origen_tipo = :o
                  AND origen_id = :id
                  AND estado IN ('PROGRAMADA', 'PENDIENTE', 'ENVIADA', 'ATENDIDA', 'ERROR')
                """;
        jdbc.sql(sql)
                .param("m", motivo)
                .param("o", origenTipo)
                .param("id", origenId)
                .update();
    }

    @Override
    public Alerta atender(UUID id, UUID empresaId, UUID actor) {
        return cambio(id, empresaId, actor, "ATENDIDA", "atendida_at", "atendida_por");
    }

    @Override
    public Alerta resolver(UUID id, UUID empresaId, UUID actor) {
        return cambio(id, empresaId, actor, "RESUELTA", "resuelta_at", "resuelta_por");
    }

    private Alerta cambio(UUID id, UUID empresaId, UUID actor, String estado, String columnaFecha, String columnaActor) {
        String sql = "UPDATE alerta SET estado = '" + estado + "', " + columnaFecha
                + " = strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), " + columnaActor
                + " = :a, updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = :id AND estado NOT IN ('RESUELTA', 'CANCELADA')";
        int n = jdbc.sql(sql)
                .param("a", actor)
                .param("id", id)
                .update();
        if (n == 0) {
            throw new BusinessException(ErrorCode.ALERTA_NOT_FOUND);
        }
        return buscar(id, empresaId).orElseThrow();
    }

    @Override
    public int activarVencidas(Instant now, int limite) {
        String sql = """
                UPDATE alerta
                SET estado = 'PENDIENTE',
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE id IN (
                    SELECT id FROM alerta
                    WHERE estado = 'PROGRAMADA' AND fecha_programada <= :n
                    ORDER BY fecha_programada
                    LIMIT :l
                )
                """;
        return jdbc.sql(sql)
                .param("n", ts(now))
                .param("l", limite)
                .update();
    }

    @Override
    public List<Alerta> listarPendientes(Instant ahora, int limite) {
        String sql = """
                SELECT * FROM alerta
                WHERE estado = 'PENDIENTE' AND fecha_programada <= :n
                ORDER BY fecha_programada
                LIMIT :l
                """;
        return jdbc.sql(sql)
                .param("n", ts(ahora))
                .param("l", limite)
                .query(this::map)
                .list();
    }

    @Override
    public void marcarEnviada(UUID id) {
        String sql = """
                UPDATE alerta
                SET estado = 'ENVIADA',
                    enviada_at = COALESCE(enviada_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE id = :id
                """;
        jdbc.sql(sql).param("id", id).update();
    }

    @Override
    public void marcarError(UUID id, String error) {
        String truncado = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        String sql = """
                UPDATE alerta
                SET estado = 'ERROR',
                    ultimo_error = :e,
                    intentos_envio = intentos_envio + 1,
                    updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE id = :id
                """;
        jdbc.sql(sql)
                .param("e", truncado)
                .param("id", id)
                .update();
    }

    private Alerta map(ResultSet r, int rowNum) throws SQLException {
        return new Alerta(
                Rows.uuid(r, "id"),
                null,
                Rows.uuid(r, "animal_id"),
                TipoAlerta.valueOf(r.getString("tipo")),
                r.getString("titulo"),
                r.getString("mensaje"),
                SeveridadAlerta.valueOf(r.getString("severidad")),
                Rows.instant(r, "fecha_programada"),
                Rows.instant(r, "fecha_vencimiento"),
                r.getString("origen_tipo"),
                Rows.uuid(r, "origen_id"),
                EstadoAlerta.valueOf(r.getString("estado")),
                read(r.getString("metadata")),
                Rows.instant(r, "enviada_at"),
                Rows.instant(r, "atendida_at"),
                Rows.instant(r, "resuelta_at"),
                Rows.instant(r, "cancelada_at"),
                Rows.uuid(r, "atendida_por"),
                Rows.uuid(r, "resuelta_por"),
                r.getString("motivo_cancelacion"),
                r.getInt("intentos_envio"),
                r.getString("ultimo_error"),
                Rows.instant(r, "created_at"),
                Rows.instant(r, "updated_at"),
                r.getString("clave_idempotencia")
        );
    }

    private String write(Map<String, Object> metadata) {
        try {
            return json.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Map<String, Object> read(String s) {
        try {
            return json.readValue(s, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String ts(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}

