package bo.com.ganadero.integraciones.telegram;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTelegramConfiguracionRepository implements TelegramConfiguracionRepository {

    private final JdbcClient jdbc;

    public JdbcTelegramConfiguracionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ConfiguracionTelegram> buscarPorEmpresa(UUID empresaId) {
        return jdbc.sql("select * from configuracion_telegram where empresa_id = :empresaId")
                .param("empresaId", empresaId.toString())
                .query(this::mapearConfig)
                .optional();
    }

    @Override
    public void guardar(ConfiguracionTelegram config) {
        jdbc.sql("""
            insert into configuracion_telegram (
                id, empresa_id, bot_token, chat_id_principal, chat_ids_json,
                activo, notificar_sanidad, notificar_reproduccion, hora_notificacion,
                created_at, updated_at, version
            ) values (
                :id, :empresaId, :botToken, :chatIdPrincipal, :chatIdsJson,
                :activo, :notificarSanidad, :notificarReproduccion, :horaNotificacion,
                :createdAt, :updatedAt, :version
            )
            on conflict(empresa_id) do update set
                bot_token = excluded.bot_token,
                chat_id_principal = excluded.chat_id_principal,
                chat_ids_json = excluded.chat_ids_json,
                activo = excluded.activo,
                notificar_sanidad = excluded.notificar_sanidad,
                notificar_reproduccion = excluded.notificar_reproduccion,
                hora_notificacion = excluded.hora_notificacion,
                updated_at = current_timestamp,
                version = version + 1
        """)
        .param("id", config.id().toString())
        .param("empresaId", config.empresaId().toString())
        .param("botToken", config.botToken())
        .param("chatIdPrincipal", config.chatIdPrincipal())
        .param("chatIdsJson", config.chatIdsJson() != null ? config.chatIdsJson() : "[]")
        .param("activo", config.activo() ? 1 : 0)
        .param("notificarSanidad", config.notificarSanidad() ? 1 : 0)
        .param("notificarReproduccion", config.notificarReproduccion() ? 1 : 0)
        .param("horaNotificacion", config.horaNotificacion() != null ? config.horaNotificacion() : "07:00")
        .param("createdAt", config.createdAt().toString())
        .param("updatedAt", Instant.now().toString())
        .param("version", config.version())
        .update();
    }

    private ConfiguracionTelegram mapearConfig(ResultSet rs, int rowNum) throws SQLException {
        return new ConfiguracionTelegram(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("empresa_id")),
            rs.getString("bot_token"),
            rs.getString("chat_id_principal"),
            rs.getString("chat_ids_json"),
            rs.getInt("activo") == 1,
            rs.getInt("notificar_sanidad") == 1,
            rs.getInt("notificar_reproduccion") == 1,
            rs.getString("hora_notificacion"),
            Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at")),
            rs.getInt("version")
        );
    }
}
