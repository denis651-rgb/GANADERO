package bo.com.ganadero.integraciones.telegram;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcOutboxAlertaRepository implements OutboxAlertaRepository {

    private final JdbcClient jdbc;

    public JdbcOutboxAlertaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void encolar(OutboxAlerta alerta) {
        jdbc.sql("""
            insert into outbox_alerta_cloud (
                id, empresa_id, tipo_modulo, subtipo, animal_id, animal_codigo,
                titulo, mensaje, fecha_programada, prioridad, estado_sincronizacion,
                intentos, ultimo_error, origen_tipo, origen_id, creado_en, sincronizado_en
            ) values (
                :id, :empresaId, :tipoModulo, :subtipo, :animalId, :animalCodigo,
                :titulo, :mensaje, :fechaProgramada, :prioridad, :estado,
                :intentos, :ultimoError, :origenTipo, :origenId, :creadoEn, :sincronizadoEn
            )
            on conflict(id) do update set
                titulo = excluded.titulo,
                mensaje = excluded.mensaje,
                fecha_programada = excluded.fecha_programada,
                prioridad = excluded.prioridad,
                estado_sincronizacion = 'PENDIENTE_SUBIDA',
                intentos = 0,
                ultimo_error = null
        """)
        .param("id", alerta.id().toString())
        .param("empresaId", alerta.empresaId() != null ? alerta.empresaId().toString() : "00000000-0000-0000-0000-000000000001")
        .param("tipoModulo", alerta.tipoModulo())
        .param("subtipo", alerta.subtipo())
        .param("animalId", alerta.animalId() != null ? alerta.animalId().toString() : null)
        .param("animalCodigo", alerta.animalCodigo())
        .param("titulo", alerta.titulo())
        .param("mensaje", alerta.mensaje())
        .param("fechaProgramada", alerta.fechaProgramada().toString())
        .param("prioridad", alerta.prioridad())
        .param("estado", alerta.estadoSincronizacion())
        .param("intentos", alerta.intentos())
        .param("ultimoError", alerta.ultimoError())
        .param("origenTipo", alerta.origenTipo())
        .param("origenId", alerta.origenId() != null ? alerta.origenId().toString() : null)
        .param("creadoEn", alerta.creadoEn().toString())
        .param("sincronizadoEn", alerta.sincronizadoEn() != null ? alerta.sincronizadoEn().toString() : null)
        .update();
    }

    @Override
    public List<OutboxAlerta> buscarPendientes(int limite) {
        return jdbc.sql("""
            select * from outbox_alerta_cloud
            where estado_sincronizacion in ('PENDIENTE_SUBIDA', 'PENDIENTE_CANCELACION')
            order by creado_en asc
            limit :limite
        """)
        .param("limite", limite)
        .query(this::mapearOutbox)
        .list();
    }

    @Override
    public Optional<OutboxAlerta> buscarPorId(UUID id) {
        return jdbc.sql("select * from outbox_alerta_cloud where id = :id")
                .param("id", id.toString())
                .query(this::mapearOutbox)
                .optional();
    }

    @Override
    public void marcarSincronizado(UUID id) {
        jdbc.sql("""
            update outbox_alerta_cloud set
                estado_sincronizacion = 'SINCRONIZADO',
                sincronizado_en = :ahora,
                ultimo_error = null
            where id = :id
        """)
        .param("id", id.toString())
        .param("ahora", Instant.now().toString())
        .update();
    }

    @Override
    public void registrarFallo(UUID id, String error, int nuevosIntentos) {
        String nuevoEstado = nuevosIntentos >= 10 ? OutboxAlerta.ESTADO_ERROR_DEFINITIVO : OutboxAlerta.ESTADO_PENDIENTE_SUBIDA;
        jdbc.sql("""
            update outbox_alerta_cloud set
                intentos = :intentos,
                ultimo_error = :error,
                estado_sincronizacion = :nuevoEstado
            where id = :id
        """)
        .param("id", id.toString())
        .param("intentos", nuevosIntentos)
        .param("error", error != null && error.length() > 500 ? error.substring(0, 500) : error)
        .param("nuevoEstado", nuevoEstado)
        .update();
    }

    @Override
    public void cancelarPorOrigen(UUID empresaId, String origenTipo, UUID origenId) {
        jdbc.sql("""
            update outbox_alerta_cloud set
                estado_sincronizacion = case
                    when estado_sincronizacion = 'SINCRONIZADO' then 'PENDIENTE_CANCELACION'
                    else 'CANCELADO'
                end
            where empresa_id = :empresaId
              and origen_tipo = :origenTipo
              and origen_id = :origenId
              and estado_sincronizacion not in ('CANCELADO')
        """)
        .param("empresaId", empresaId.toString())
        .param("origenTipo", origenTipo)
        .param("origenId", origenId.toString())
        .update();
    }

    @Override
    public void marcarCancelado(UUID id) {
        jdbc.sql("update outbox_alerta_cloud set estado_sincronizacion = 'CANCELADO' where id = :id")
                .param("id", id.toString())
                .update();
    }

    @Override
    public int contarPendientes(UUID empresaId) {
        return jdbc.sql("""
            select count(*) from outbox_alerta_cloud
            where empresa_id = :empresaId
              and estado_sincronizacion in ('PENDIENTE_SUBIDA', 'PENDIENTE_CANCELACION')
        """)
        .param("empresaId", empresaId.toString())
        .query(Integer.class)
        .single();
    }

    private OutboxAlerta mapearOutbox(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxAlerta(
            parseUuidSeguro(rs.getString("id")),
            parseUuidSeguro(rs.getString("empresa_id")),
            rs.getString("tipo_modulo"),
            rs.getString("subtipo"),
            rs.getString("animal_id") != null && !rs.getString("animal_id").isBlank() ? parseUuidSeguro(rs.getString("animal_id")) : null,
            rs.getString("animal_codigo"),
            rs.getString("titulo"),
            rs.getString("mensaje"),
            parseInstantSeguro(rs.getString("fecha_programada")),
            rs.getString("prioridad"),
            rs.getString("estado_sincronizacion"),
            rs.getInt("intentos"),
            rs.getString("ultimo_error"),
            rs.getString("origen_tipo"),
            rs.getString("origen_id") != null && !rs.getString("origen_id").isBlank() ? parseUuidSeguro(rs.getString("origen_id")) : null,
            parseInstantSeguro(rs.getString("creado_en")),
            parseInstantSeguro(rs.getString("sincronizado_en"))
        );
    }

    private UUID parseUuidSeguro(String text) {
        if (text == null || text.isBlank()) {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        try {
            return UUID.fromString(text.trim());
        } catch (Exception e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
    }

    private Instant parseInstantSeguro(String text) {
        if (text == null || text.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(text.trim());
        } catch (Exception e) {
            try {
                String iso = text.trim().replace(" ", "T");
                if (!iso.endsWith("Z") && !iso.contains("+")) {
                    iso = iso + "Z";
                }
                return Instant.parse(iso);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}
