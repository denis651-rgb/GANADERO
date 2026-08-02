package bo.com.ganadero.shared.security;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true")
public class JwtCurrentUserProvider implements CurrentUserProvider {
    private final JdbcClient jdbcClient;

    public JwtCurrentUserProvider(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public CurrentUser get() {
        Jwt jwt = authenticatedJwt();
        UUID userId = parseUuid(jwt.getSubject());
        UUID requestedEmpresaId = optionalUuid(jwt.getClaimAsString("empresa_id"));
        Membership membership = findMembership(userId, requestedEmpresaId);

        Set<String> roles = strings("""
                select distinct r.codigo from seguridad.usuario_roles ur
                join seguridad.roles r on r.id = ur.rol_id
                where ur.miembro_empresa_id = :memberId and r.activo
                """, membership.memberId());
        Set<String> permissions = strings("""
                select distinct p.codigo from seguridad.usuario_roles ur
                join seguridad.rol_permisos rp on rp.rol_id = ur.rol_id
                join seguridad.permisos p on p.id = rp.permiso_id
                where ur.miembro_empresa_id = :memberId and p.activo
                """, membership.memberId());
        Set<UUID> properties = membership.allProperties() ? Set.of() : new HashSet<>(jdbcClient.sql("""
                select propiedad_id from seguridad.usuario_propiedades
                where miembro_empresa_id = :memberId
                """).param("memberId", membership.memberId()).query(UUID.class).list());

        return new CurrentUser(userId, membership.empresaId(), membership.memberId(), roles, permissions, properties,
                membership.allProperties());
    }

    private Jwt authenticatedJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return jwt;
    }

    private Membership findMembership(UUID userId, UUID empresaId) {
        String companyFilter = empresaId == null ? "" : " and me.empresa_id = :empresaId";
        JdbcClient.StatementSpec statement = jdbcClient.sql("""
                select me.id, me.empresa_id, me.acceso_todas_propiedades
                from seguridad.miembros_empresa me
                join seguridad.perfiles_usuario pu on pu.id = me.usuario_id
                where me.usuario_id = :userId and me.estado = 'ACTIVO' and pu.activo
                """ + companyFilter + " order by me.fecha_ingreso nulls last limit 1").param("userId", userId);
        if (empresaId != null) statement = statement.param("empresaId", empresaId);
        return statement.query((rs, rowNum) -> new Membership(
                rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                rs.getBoolean("acceso_todas_propiedades")))
                .optional().orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    private Set<String> strings(String sql, UUID memberId) {
        return new HashSet<>(jdbcClient.sql(sql).param("memberId", memberId).query(String.class).list());
    }

    private UUID parseUuid(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException exception) { throw new BusinessException(ErrorCode.UNAUTHENTICATED); }
    }

    private UUID optionalUuid(String value) {
        return value == null || value.isBlank() ? null : parseUuid(value);
    }

    private record Membership(UUID memberId, UUID empresaId, boolean allProperties) {}
}
