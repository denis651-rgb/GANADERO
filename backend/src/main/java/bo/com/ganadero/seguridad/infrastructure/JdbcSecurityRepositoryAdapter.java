package bo.com.ganadero.seguridad.infrastructure;

import bo.com.ganadero.seguridad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
class JdbcSecurityRepositoryAdapter implements PerfilUsuarioRepository, MiembroEmpresaRepository, RolRepository {
    private final JdbcClient jdbc;

    JdbcSecurityRepositoryAdapter(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<PerfilUsuario> findById(UUID usuarioId) {
        return jdbc.sql("select * from seguridad.perfiles_usuario where id=:id")
                .param("id", usuarioId).query(this::profile).optional();
    }

    @Override
    public PerfilUsuario createIfAbsent(UUID usuarioId, String nombres, String apellidos,
                                        String telefono, UUID actorId) {
        jdbc.sql("""
                insert into seguridad.perfiles_usuario
                    (id,nombres,apellidos,telefono,activo,created_by,updated_by)
                values (:id,:nombres,:apellidos,:telefono,true,:actor,:actor)
                on conflict (id) do nothing
                """).param("id", usuarioId).param("nombres", nombres).param("apellidos", apellidos)
                .param("telefono", telefono).param("actor", actorId).update();
        return findById(usuarioId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public PerfilUsuario update(UUID usuarioId, String nombres, String apellidos, String telefono,
                                String avatarPath, long version, UUID actorId) {
        int changed = jdbc.sql("""
                update seguridad.perfiles_usuario set
                    nombres=coalesce(:nombres,nombres), apellidos=coalesce(:apellidos,apellidos),
                    telefono=coalesce(:telefono,telefono), avatar_path=coalesce(:avatar,avatar_path),
                    updated_at=now(), updated_by=:actor, version=version+1
                where id=:id and version=:version
                """).param("nombres", nombres).param("apellidos", apellidos).param("telefono", telefono)
                .param("avatar", avatarPath).param("actor", actorId).param("id", usuarioId)
                .param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(usuarioId).isPresent());
        return findById(usuarioId).orElseThrow();
    }

    @Override
    public Optional<MiembroEmpresa> findByIdAndEmpresaId(UUID miembroId, UUID empresaId) {
        return jdbc.sql(memberSql() + " where me.id=:id and me.empresa_id=:empresaId")
                .param("id", miembroId).param("empresaId", empresaId).query(this::member).optional();
    }

    @Override
    public List<MiembroEmpresa> findAllByEmpresaId(UUID empresaId) {
        return jdbc.sql(memberSql() + " where me.empresa_id=:empresaId order by pu.apellidos, pu.nombres")
                .param("empresaId", empresaId).query(this::member).list();
    }

    @Override
    public MiembroEmpresa create(UUID empresaId, UUID usuarioId, String cargo,
                                 boolean accesoTotal, UUID actorId) {
        boolean exists = jdbc.sql("select count(*) from seguridad.miembros_empresa where empresa_id=:e and usuario_id=:u")
                .param("e", empresaId).param("u", usuarioId).query(Long.class).single() > 0;
        if (exists) throw new BusinessException(ErrorCode.USER_ALREADY_MEMBER);
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into seguridad.miembros_empresa
                    (id,empresa_id,usuario_id,cargo,estado,fecha_ingreso,acceso_todas_propiedades,created_by,updated_by)
                values (:id,:empresa,:usuario,:cargo,'ACTIVO',current_date,:acceso,:actor,:actor)
                """).param("id", id).param("empresa", empresaId).param("usuario", usuarioId)
                .param("cargo", cargo).param("acceso", accesoTotal).param("actor", actorId).update();
        return findByIdAndEmpresaId(id, empresaId).orElseThrow();
    }

    @Override
    public MiembroEmpresa update(UUID miembroId, UUID empresaId, String cargo, Boolean accesoTotal,
                                 long version, UUID actorId) {
        int changed = jdbc.sql("""
                update seguridad.miembros_empresa set cargo=coalesce(:cargo,cargo),
                    acceso_todas_propiedades=coalesce(:acceso,acceso_todas_propiedades),
                    updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and empresa_id=:empresa and version=:version
                """).param("cargo", cargo).param("acceso", accesoTotal).param("actor", actorId)
                .param("id", miembroId).param("empresa", empresaId).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findByIdAndEmpresaId(miembroId, empresaId).isPresent());
        return findByIdAndEmpresaId(miembroId, empresaId).orElseThrow();
    }

    @Override
    public MiembroEmpresa changeStatus(UUID miembroId, UUID empresaId, EstadoMiembro estado,
                                       long version, UUID actorId) {
        int changed = jdbc.sql("""
                update seguridad.miembros_empresa set estado=:estado,updated_at=now(),updated_by=:actor,
                    version=version+1 where id=:id and empresa_id=:empresa and version=:version
                """).param("estado", estado.name()).param("actor", actorId).param("id", miembroId)
                .param("empresa", empresaId).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findByIdAndEmpresaId(miembroId, empresaId).isPresent());
        return findByIdAndEmpresaId(miembroId, empresaId).orElseThrow();
    }

    @Override
    public void replaceRoles(UUID miembroId, UUID empresaId, Set<UUID> roleIds, long version, UUID actorId) {
        requireMember(miembroId, empresaId);
        long valid = roleIds.isEmpty() ? 0 : jdbc.sql("""
                select count(*) from seguridad.roles where id in (:ids) and activo
                    and (empresa_id is null or empresa_id=:empresa)
                """).param("ids", roleIds).param("empresa", empresaId).query(Long.class).single();
        if (valid != roleIds.size()) throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        jdbc.sql("delete from seguridad.usuario_roles where miembro_empresa_id=:id").param("id", miembroId).update();
        roleIds.forEach(roleId -> jdbc.sql("insert into seguridad.usuario_roles(miembro_empresa_id,rol_id) values(:m,:r)")
                .param("m", miembroId).param("r", roleId).update());
        touchMember(miembroId, version, actorId);
    }

    @Override
    public void replaceProperties(UUID miembroId, UUID empresaId, Set<UUID> propertyIds, long version, UUID actorId) {
        requireMember(miembroId, empresaId);
        long valid = propertyIds.isEmpty() ? 0 : jdbc.sql("select count(*) from core.propiedades where id in (:ids) and empresa_id=:empresa and activo")
                .param("ids", propertyIds).param("empresa", empresaId).query(Long.class).single();
        if (valid != propertyIds.size()) throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
        jdbc.sql("delete from seguridad.usuario_propiedades where miembro_empresa_id=:id")
                .param("id", miembroId).update();
        propertyIds.forEach(propertyId -> jdbc.sql("""
                insert into seguridad.usuario_propiedades(miembro_empresa_id,propiedad_id) values(:m,:p)
                """).param("m", miembroId).param("p", propertyId).update());
        touchMember(miembroId, version, actorId);
    }

    @Override
    public long countActiveOwners(UUID empresaId) {
        return jdbc.sql("""
                select count(distinct me.id) from seguridad.miembros_empresa me
                join seguridad.usuario_roles ur on ur.miembro_empresa_id=me.id
                join seguridad.roles r on r.id=ur.rol_id
                where me.empresa_id=:empresa and me.estado='ACTIVO' and r.codigo='PROPIETARIO'
                """).param("empresa", empresaId).query(Long.class).single();
    }

    @Override
    public Optional<UsuarioActual> findCurrentUser(UUID usuarioId, UUID empresaId) {
        return jdbc.sql("""
                select pu.id,pu.nombres,pu.apellidos,e.id empresa_id,e.nombre_comercial
                from seguridad.perfiles_usuario pu
                join seguridad.miembros_empresa me on me.usuario_id=pu.id
                join core.empresas e on e.id=me.empresa_id
                where pu.id=:usuario and e.id=:empresa and pu.activo and me.estado='ACTIVO'
                """).param("usuario", usuarioId).param("empresa", empresaId).query((rs, n) -> {
                    UUID memberId = jdbc.sql("select id from seguridad.miembros_empresa where usuario_id=:u and empresa_id=:e")
                            .param("u", usuarioId).param("e", empresaId).query(UUID.class).single();
                    Set<Rol> roles = rolesForMember(memberId);
                    Set<String> roleCodes = new HashSet<>(); Set<String> permissions = new HashSet<>();
                    roles.forEach(role -> { roleCodes.add(role.codigo()); role.permisos().forEach(p -> permissions.add(p.codigo())); });
                    Set<UUID> properties = propertiesForMember(memberId);
                    return new UsuarioActual(rs.getObject("id", UUID.class), rs.getString("nombres"),
                            rs.getString("apellidos"), rs.getObject("empresa_id", UUID.class),
                            rs.getString("nombre_comercial"), roleCodes, permissions, properties);
                }).optional();
    }

    @Override
    public List<Rol> findAllAvailableFor(UUID empresaId) {
        return jdbc.sql("select * from seguridad.roles where empresa_id is null or empresa_id=:empresa order by codigo")
                .param("empresa", empresaId).query(this::role).list();
    }

    @Override
    public Optional<Rol> findAvailableById(UUID roleId, UUID empresaId) {
        return jdbc.sql("select * from seguridad.roles where id=:id and (empresa_id is null or empresa_id=:empresa)")
                .param("id", roleId).param("empresa", empresaId).query(this::role).optional();
    }

    @Override
    public List<Permiso> findAllPermissions() {
        return jdbc.sql("select * from seguridad.permisos where activo order by modulo,codigo")
                .query(this::permission).list();
    }

    @Override
    public Rol create(UUID empresaId, String codigo, String nombre, String descripcion, UUID actorId) {
        boolean exists = jdbc.sql("select count(*) from seguridad.roles where codigo=:codigo and (empresa_id=:e or empresa_id is null)")
                .param("codigo", codigo).param("e", empresaId).query(Long.class).single() > 0;
        if (exists) throw new BusinessException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into seguridad.roles(id,empresa_id,codigo,nombre,descripcion,es_sistema,activo,created_by,updated_by)
                values(:id,:empresa,:codigo,:nombre,:descripcion,false,true,:actor,:actor)
                """).param("id", id).param("empresa", empresaId).param("codigo", codigo)
                .param("nombre", nombre).param("descripcion", descripcion).param("actor", actorId).update();
        return findAvailableById(id, empresaId).orElseThrow();
    }

    @Override
    public Rol update(UUID roleId, UUID empresaId, String nombre, String descripcion, Boolean activo,
                      long version, UUID actorId) {
        Rol role = findAvailableById(roleId, empresaId).orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        int changed = jdbc.sql("""
                update seguridad.roles set nombre=coalesce(:nombre,nombre),descripcion=coalesce(:descripcion,descripcion),
                    activo=coalesce(:activo,activo),updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and version=:version
                """).param("nombre", nombre).param("descripcion", descripcion).param("activo", activo)
                .param("actor", actorId).param("id", roleId).param("version", version).update();
        if (changed == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return findAvailableById(roleId, empresaId).orElseThrow();
    }

    @Override
    public Rol replacePermissions(UUID roleId, UUID empresaId, Set<UUID> permissionIds, long version, UUID actorId) {
        findAvailableById(roleId, empresaId).orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        long valid = permissionIds.isEmpty() ? 0 : jdbc.sql("select count(*) from seguridad.permisos where id in (:ids) and activo")
                .param("ids", permissionIds).query(Long.class).single();
        if (valid != permissionIds.size()) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Uno o más permisos no existen o están inactivos.");
        jdbc.sql("delete from seguridad.rol_permisos where rol_id=:id").param("id", roleId).update();
        permissionIds.forEach(permissionId -> jdbc.sql("insert into seguridad.rol_permisos(rol_id,permiso_id) values(:r,:p)")
                .param("r", roleId).param("p", permissionId).update());
        int changed=jdbc.sql("update seguridad.roles set updated_at=now(),updated_by=:actor,version=version+1 where id=:id and version=:version")
                .param("actor", actorId).param("id", roleId).param("version",version).update();
        if(changed==0)throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return findAvailableById(roleId, empresaId).orElseThrow();
    }

    private PerfilUsuario profile(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new PerfilUsuario(rs.getObject("id",UUID.class),rs.getString("nombres"),rs.getString("apellidos"),
                rs.getString("telefono"),rs.getString("avatar_path"),rs.getBoolean("activo"),
                instant(rs,"ultimo_acceso_at"),instant(rs,"created_at"),
                instant(rs,"updated_at"),rs.getLong("version"));
    }

    private MiembroEmpresa member(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        UUID memberId=rs.getObject("miembro_id",UUID.class);
        PerfilUsuario profile=new PerfilUsuario(rs.getObject("usuario_id",UUID.class),rs.getString("nombres"),
                rs.getString("apellidos"),rs.getString("telefono"),rs.getString("avatar_path"),rs.getBoolean("usuario_activo"),
                instant(rs,"ultimo_acceso_at"),instant(rs,"perfil_created_at"),
                instant(rs,"perfil_updated_at"),rs.getLong("perfil_version"));
        return new MiembroEmpresa(memberId,rs.getObject("empresa_id",UUID.class),profile,rs.getString("cargo"),
                EstadoMiembro.valueOf(rs.getString("estado")),rs.getObject("fecha_ingreso",LocalDate.class),
                rs.getBoolean("acceso_todas_propiedades"),instant(rs,"created_at"),
                rs.getObject("created_by",UUID.class),instant(rs,"updated_at"),
                rs.getObject("updated_by",UUID.class),rs.getLong("version"),rolesForMember(memberId),propertiesForMember(memberId));
    }

    private Rol role(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        UUID id=rs.getObject("id",UUID.class);
        return new Rol(id,rs.getObject("empresa_id",UUID.class),rs.getString("codigo"),rs.getString("nombre"),
                rs.getString("descripcion"),rs.getBoolean("es_sistema"),rs.getBoolean("activo"),
                instant(rs,"created_at"),instant(rs,"updated_at"),
                rs.getLong("version"),permissionsForRole(id));
    }

    private Permiso permission(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Permiso(rs.getObject("id",UUID.class),rs.getString("codigo"),rs.getString("nombre"),
                rs.getString("descripcion"),rs.getString("modulo"),rs.getBoolean("activo"));
    }

    private Set<Rol> rolesForMember(UUID memberId) {
        return new HashSet<>(jdbc.sql("select r.* from seguridad.roles r join seguridad.usuario_roles ur on ur.rol_id=r.id where ur.miembro_empresa_id=:id order by r.codigo")
                .param("id",memberId).query(this::role).list());
    }
    private Set<Permiso> permissionsForRole(UUID roleId) {
        return new HashSet<>(jdbc.sql("select p.* from seguridad.permisos p join seguridad.rol_permisos rp on rp.permiso_id=p.id where rp.rol_id=:id order by p.codigo")
                .param("id",roleId).query(this::permission).list());
    }
    private Set<UUID> propertiesForMember(UUID memberId) {
        return new HashSet<>(jdbc.sql("select propiedad_id from seguridad.usuario_propiedades where miembro_empresa_id=:id")
                .param("id",memberId).query(UUID.class).list());
    }
    private void requireMember(UUID memberId,UUID empresaId) {
        if(findByIdAndEmpresaId(memberId,empresaId).isEmpty()) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    private void touchMember(UUID memberId,long version,UUID actorId) {
        int changed=jdbc.sql("update seguridad.miembros_empresa set updated_at=now(),updated_by=:a,version=version+1 where id=:id and version=:version")
                .param("a",actorId).param("id",memberId).param("version",version).update();
        if(changed==0)throw new BusinessException(ErrorCode.VERSION_CONFLICT);
    }
    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.USER_NOT_FOUND);
    }
    private Instant instant(java.sql.ResultSet rs,String column) throws java.sql.SQLException {
        java.time.OffsetDateTime value=rs.getObject(column,java.time.OffsetDateTime.class);
        return value==null?null:value.toInstant();
    }
    private String memberSql() { return """
        select me.id miembro_id,me.empresa_id,me.cargo,me.estado,me.fecha_ingreso,me.acceso_todas_propiedades,
        me.created_at,me.created_by,me.updated_at,me.updated_by,me.version,
        pu.id usuario_id,pu.nombres,pu.apellidos,pu.telefono,pu.avatar_path,pu.activo usuario_activo,
        pu.ultimo_acceso_at,pu.created_at perfil_created_at,pu.updated_at perfil_updated_at,pu.version perfil_version
        from seguridad.miembros_empresa me join seguridad.perfiles_usuario pu on pu.id=me.usuario_id
        """; }
}
