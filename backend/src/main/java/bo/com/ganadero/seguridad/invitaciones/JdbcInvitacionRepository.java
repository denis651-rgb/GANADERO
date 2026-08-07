package bo.com.ganadero.seguridad.invitaciones;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcInvitacionRepository implements InvitacionRepository {

    private static final String BASE_SELECT = """
            select id, empresa_id, miembro_empresa_id, usuario_id, email, estado,
                   fecha_envio, fecha_vencimiento, fecha_aceptacion, fecha_cancelacion,
                   intentos_envio, ultimo_error_codigo, ultimo_error_mensaje,
                   invitado_por, cancelado_por, motivo_cancelacion,
                   created_at, updated_at, version
            from seguridad.invitaciones_usuario
            """;

    private final JdbcClient jdbc;

    JdbcInvitacionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public InvitacionUsuario insert(UUID empresaId, String email, UUID invitadoPor, Instant fechaVencimiento) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                        insert into seguridad.invitaciones_usuario
                            (id, empresa_id, email, estado, fecha_vencimiento, invitado_por)
                        values (:id, :empresa, :email, 'PENDIENTE', :vencimiento, :invitado)
                        """)
                .param("id", id)
                .param("empresa", empresaId)
                .param("email", email)
                .param("vencimiento", Timestamp.from(fechaVencimiento))
                .param("invitado", invitadoPor)
                .update();
        return findByIdAndEmpresaId(id, empresaId).orElseThrow();
    }

    @Override
    public Optional<InvitacionUsuario> findByIdAndEmpresaId(UUID id, UUID empresaId) {
        return jdbc.sql(BASE_SELECT + " where id = :id and empresa_id = :empresa")
                .param("id", id)
                .param("empresa", empresaId)
                .query(this::map)
                .optional();
    }

    @Override
    public Optional<InvitacionUsuario> findActiveByEmpresaAndEmail(UUID empresaId, String email) {
        return jdbc.sql(BASE_SELECT + """
                        where empresa_id = :empresa
                          and lower(email) = lower(:email)
                          and estado in ('PENDIENTE', 'ERROR_ENVIO')
                        order by created_at desc
                        limit 1
                        """)
                .param("empresa", empresaId)
                .param("email", email)
                .query(this::map)
                .optional();
    }

    @Override
    public Optional<InvitacionUsuario> findByUsuarioId(UUID usuarioId) {
        return jdbc.sql(BASE_SELECT + """
                        where usuario_id = :usuario
                          and estado in ('PENDIENTE', 'ERROR_ENVIO')
                        order by created_at desc
                        limit 1
                        """)
                .param("usuario", usuarioId)
                .query(this::map)
                .optional();
    }

    @Override
    public List<InvitacionUsuario> search(UUID empresaId, InvitacionFiltro filtro) {
        Map<String, Object> params = new HashMap<>();
        params.put("empresa", empresaId);
        String where = buildWhere(filtro, params);
        return jdbc.sql(BASE_SELECT + where + " order by created_at desc offset :offset limit :limit")
                .params(params)
                .param("offset", (long) filtro.page() * filtro.size())
                .param("limit", filtro.size())
                .query(this::map)
                .list();
    }

    @Override
    public long count(UUID empresaId, InvitacionFiltro filtro) {
        Map<String, Object> params = new HashMap<>();
        params.put("empresa", empresaId);
        String where = buildWhere(filtro, params);
        return jdbc.sql("select count(*) from seguridad.invitaciones_usuario" + where)
                .params(params)
                .query(Long.class)
                .single();
    }

    private String buildWhere(InvitacionFiltro filtro, Map<String, Object> params) {
        StringBuilder where = new StringBuilder(" where empresa_id = :empresa");
        if (filtro.estado() != null && !filtro.estado().isBlank()) {
            where.append(" and estado = :estado");
            params.put("estado", filtro.estado().toUpperCase());
        }
        if (filtro.email() != null && !filtro.email().isBlank()) {
            where.append(" and lower(email) like :email");
            params.put("email", "%" + filtro.email().toLowerCase() + "%");
        }
        if (filtro.desde() != null) {
            where.append(" and created_at >= :desde");
            params.put("desde", Timestamp.from(filtro.desde().toInstant()));
        }
        if (filtro.hasta() != null) {
            where.append(" and created_at <= :hasta");
            params.put("hasta", Timestamp.from(filtro.hasta().toInstant()));
        }
        return where.toString();
    }

    @Override
    @Transactional
    public InvitacionUsuario markEnviada(UUID id, UUID empresaId, long version, UUID usuarioId,
                                         UUID miembroId, Instant fechaEnvio, Instant fechaVencimiento) {
        int changed = jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set usuario_id = :usuario,
                            miembro_empresa_id = :miembro,
                            estado = 'PENDIENTE',
                            fecha_envio = :envio,
                            fecha_vencimiento = :vencimiento,
                            intentos_envio = 1,
                            ultimo_error_codigo = null,
                            ultimo_error_mensaje = null,
                            updated_at = now(),
                            version = version + 1
                        where id = :id and empresa_id = :empresa and version = :version
                        """)
                .param("usuario", usuarioId)
                .param("miembro", miembroId)
                .param("envio", Timestamp.from(fechaEnvio))
                .param("vencimiento", Timestamp.from(fechaVencimiento))
                .param("id", id)
                .param("empresa", empresaId)
                .param("version", version)
                .update();
        return expectChanged(changed, id, empresaId);
    }

    @Override
    @Transactional
    public InvitacionUsuario marcarErrorEnvio(UUID id, UUID empresaId, long version,
                                              String codigo, String mensaje) {
        int changed = jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set estado = 'ERROR_ENVIO',
                            ultimo_error_codigo = :codigo,
                            ultimo_error_mensaje = :mensaje,
                            updated_at = now(),
                            version = version + 1
                        where id = :id and empresa_id = :empresa and version = :version
                        """)
                .param("codigo", codigo)
                .param("mensaje", truncar(mensaje, 500))
                .param("id", id)
                .param("empresa", empresaId)
                .param("version", version)
                .update();
        return expectChanged(changed, id, empresaId);
    }

    @Override
    @Transactional
    public InvitacionUsuario resend(UUID id, UUID empresaId, long version,
                                    Instant fechaEnvio, Instant fechaVencimiento) {
        int changed = jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set estado = 'PENDIENTE',
                            fecha_envio = :envio,
                            fecha_vencimiento = :vencimiento,
                            intentos_envio = intentos_envio + 1,
                            ultimo_error_codigo = null,
                            ultimo_error_mensaje = null,
                            updated_at = now(),
                            version = version + 1
                        where id = :id and empresa_id = :empresa and version = :version
                        """)
                .param("envio", Timestamp.from(fechaEnvio))
                .param("vencimiento", Timestamp.from(fechaVencimiento))
                .param("id", id)
                .param("empresa", empresaId)
                .param("version", version)
                .update();
        return expectChanged(changed, id, empresaId);
    }

    @Override
    @Transactional
    public InvitacionUsuario cancel(UUID id, UUID empresaId, long version, UUID canceladoPor,
                                    String motivo, Instant ahora) {
        int changed = jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set estado = 'CANCELADA',
                            fecha_cancelacion = :ahora,
                            cancelado_por = :cancelado,
                            motivo_cancelacion = :motivo,
                            updated_at = now(),
                            version = version + 1
                        where id = :id and empresa_id = :empresa and version = :version
                        """)
                .param("ahora", Timestamp.from(ahora))
                .param("cancelado", canceladoPor)
                .param("motivo", truncar(motivo, 300))
                .param("id", id)
                .param("empresa", empresaId)
                .param("version", version)
                .update();
        return expectChanged(changed, id, empresaId);
    }

    @Override
    @Transactional
    public InvitacionUsuario accept(UUID id, UUID empresaId, long version, Instant ahora) {
        int changed = jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set estado = 'ACEPTADA',
                            fecha_aceptacion = :ahora,
                            updated_at = now(),
                            version = version + 1
                        where id = :id and empresa_id = :empresa and version = :version
                        """)
                .param("ahora", Timestamp.from(ahora))
                .param("id", id)
                .param("empresa", empresaId)
                .param("version", version)
                .update();
        return expectChanged(changed, id, empresaId);
    }

    @Override
    public List<InvitacionUsuario> findPendingExpired(Instant now) {
        return jdbc.sql(BASE_SELECT + """
                        where estado in ('PENDIENTE', 'ERROR_ENVIO')
                          and fecha_vencimiento < :now
                        order by created_at
                        """)
                .param("now", Timestamp.from(now))
                .query(this::map)
                .list();
    }

    @Override
    @Transactional
    public InvitacionUsuario expire(UUID id, UUID empresaId, long version, Instant ahora) {
        int changed = jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set estado = 'VENCIDA',
                            updated_at = now(),
                            version = version + 1
                        where id = :id and empresa_id = :empresa and version = :version
                        """)
                .param("id", id)
                .param("empresa", empresaId)
                .param("version", version)
                .update();
        return expectChanged(changed, id, empresaId);
    }

    @Override
    @Transactional
    public int markExpired(Instant now) {
        return jdbc.sql("""
                        update seguridad.invitaciones_usuario
                        set estado = 'VENCIDA',
                            updated_at = now()
                        where estado in ('PENDIENTE', 'ERROR_ENVIO')
                          and fecha_vencimiento < :now
                        """)
                .param("now", Timestamp.from(now))
                .update();
    }

    private InvitacionUsuario expectChanged(int changed, UUID id, UUID empresaId) {
        if (changed == 0 && findByIdAndEmpresaId(id, empresaId).isEmpty()) {
            throw new BusinessException(ErrorCode.INVITACION_NOT_FOUND);
        }
        return findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> new BusinessException(ErrorCode.VERSION_CONFLICT));
    }

    private InvitacionUsuario map(ResultSet rs, int rowNum) throws SQLException {
        return new InvitacionUsuario(
                rs.getObject("id", UUID.class),
                rs.getObject("empresa_id", UUID.class),
                rs.getObject("miembro_empresa_id", UUID.class),
                rs.getObject("usuario_id", UUID.class),
                rs.getString("email"),
                EstadoInvitacion.valueOf(rs.getString("estado")),
                instant(rs, "fecha_envio"),
                instant(rs, "fecha_vencimiento"),
                instant(rs, "fecha_aceptacion"),
                instant(rs, "fecha_cancelacion"),
                rs.getInt("intentos_envio"),
                rs.getString("ultimo_error_codigo"),
                rs.getString("ultimo_error_mensaje"),
                rs.getObject("invitado_por", UUID.class),
                rs.getObject("cancelado_por", UUID.class),
                rs.getString("motivo_cancelacion"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                rs.getLong("version"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private String truncar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
