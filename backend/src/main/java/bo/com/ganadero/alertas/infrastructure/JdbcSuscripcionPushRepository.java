package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcSuscripcionPushRepository implements SuscripcionPushRepository {
    private final JdbcClient jdbc;

    public JdbcSuscripcionPushRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public SuscripcionPush guardar(SuscripcionPush suscripcion) {
        UUID id = jdbc.sql("""
                        insert into alertas.suscripciones_push(
                            id,empresa_id,usuario_id,endpoint,p256dh,auth,dispositivo_nombre,user_agent,activo,ultimo_uso_at
                        ) values(:id,:empresa,:usuario,:endpoint,:p256dh,:auth,:dispositivo,:agente,true,now())
                        on conflict(usuario_id,endpoint) do update set
                            empresa_id=excluded.empresa_id,p256dh=excluded.p256dh,auth=excluded.auth,
                            dispositivo_nombre=excluded.dispositivo_nombre,user_agent=excluded.user_agent,
                            activo=true,ultimo_uso_at=now(),updated_at=now()
                        returning id
                        """)
                .param("id", suscripcion.id()).param("empresa", suscripcion.empresaId())
                .param("usuario", suscripcion.usuarioId()).param("endpoint", suscripcion.endpoint())
                .param("p256dh", suscripcion.p256dh()).param("auth", suscripcion.auth())
                .param("dispositivo", suscripcion.dispositivoNombre()).param("agente", suscripcion.userAgent())
                .query(UUID.class).single();
        return listar(suscripcion.empresaId(), suscripcion.usuarioId()).stream()
                .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public List<SuscripcionPush> listar(UUID empresa, UUID usuario) {
        return jdbc.sql("select * from alertas.suscripciones_push where empresa_id=:e and usuario_id=:u and activo order by ultimo_uso_at desc nulls last")
                .param("e", empresa).param("u", usuario).query(this::map).list();
    }

    public List<SuscripcionPush> listarActivas(UUID empresa) {
        return jdbc.sql("select * from alertas.suscripciones_push where empresa_id=:e and activo order by ultimo_uso_at desc nulls last")
                .param("e", empresa).query(this::map).list();
    }

    public void desactivar(UUID id, UUID empresa, UUID usuario) {
        jdbc.sql("update alertas.suscripciones_push set activo=false,updated_at=now() where id=:id and empresa_id=:e and usuario_id=:u")
                .param("id", id).param("e", empresa).param("u", usuario).update();
    }

    public void desactivarTodas(UUID id, UUID empresa) {
        jdbc.sql("update alertas.suscripciones_push set activo=false,updated_at=now() where id=:id and empresa_id=:e")
                .param("id", id).param("e", empresa).update();
    }

    public PreferenciasNotificacion preferencias(UUID empresa, UUID usuario) {
        return jdbc.sql("select * from alertas.preferencias_notificacion where empresa_id=:e and usuario_id=:u")
                .param("e", empresa).param("u", usuario).query(this::mapPreferencias).optional()
                .orElse(new PreferenciasNotificacion(empresa, usuario, true, true, true, true,
                        true, true, true, true, true, true, true));
    }

    public PreferenciasNotificacion guardarPreferencias(PreferenciasNotificacion preferencias) {
        jdbc.sql("""
                        insert into alertas.preferencias_notificacion(
                            empresa_id,usuario_id,reproduccion,sanidad,tratamientos,pesajes,movimientos,
                            inventario,sistema,casos_criticos,criticas,urgentes,recordatorios,updated_at
                        ) values(:e,:u,:r,:s,:t,:p,:m,:i,:si,:c,:cr,:ur,:re,now())
                        on conflict(empresa_id,usuario_id) do update set
                            reproduccion=excluded.reproduccion,sanidad=excluded.sanidad,
                            tratamientos=excluded.tratamientos,pesajes=excluded.pesajes,
                            movimientos=excluded.movimientos,inventario=excluded.inventario,sistema=excluded.sistema,
                            casos_criticos=excluded.casos_criticos,criticas=excluded.criticas,
                            urgentes=excluded.urgentes,recordatorios=excluded.recordatorios,updated_at=now()
                        """)
                .param("e", preferencias.empresaId()).param("u", preferencias.usuarioId())
                .param("r", preferencias.reproduccion()).param("s", preferencias.sanidad())
                .param("t", preferencias.tratamientos()).param("p", preferencias.pesajes())
                .param("m", preferencias.movimientos()).param("i", preferencias.inventario())
                .param("si", preferencias.sistema()).param("c", preferencias.casosCriticos())
                .param("cr", preferencias.criticas()).param("ur", preferencias.urgentes())
                .param("re", preferencias.recordatorios()).update();
        return preferencias;
    }

    private SuscripcionPush map(ResultSet result, int row) throws SQLException {
        return new SuscripcionPush(result.getObject("id", UUID.class), result.getObject("empresa_id", UUID.class),
                result.getObject("usuario_id", UUID.class), result.getString("endpoint"), result.getString("p256dh"),
                result.getString("auth"), result.getString("dispositivo_nombre"), result.getString("user_agent"),
                result.getBoolean("activo"), instant(result, "created_at"), instant(result, "updated_at"),
                instant(result, "ultimo_uso_at"));
    }

    private PreferenciasNotificacion mapPreferencias(ResultSet result, int row) throws SQLException {
        return new PreferenciasNotificacion(result.getObject("empresa_id", UUID.class),
                result.getObject("usuario_id", UUID.class), result.getBoolean("reproduccion"),
                result.getBoolean("sanidad"), result.getBoolean("tratamientos"), result.getBoolean("pesajes"),
                result.getBoolean("movimientos"), result.getBoolean("inventario"), result.getBoolean("sistema"),
                result.getBoolean("casos_criticos"), result.getBoolean("criticas"), result.getBoolean("urgentes"),
                result.getBoolean("recordatorios"));
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
