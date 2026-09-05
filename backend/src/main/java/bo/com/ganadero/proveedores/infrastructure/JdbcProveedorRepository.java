package bo.com.ganadero.proveedores.infrastructure;

import bo.com.ganadero.proveedores.domain.Proveedor;
import bo.com.ganadero.proveedores.domain.ProveedorRepository;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcProveedorRepository implements ProveedorRepository {
    private final JdbcClient jdbc;

    JdbcProveedorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Proveedor> buscar(String query, boolean soloActivos) {
        StringBuilder sql = new StringBuilder("select * from proveedor where 1=1");
        Map<String, Object> params = new java.util.HashMap<>();
        if (soloActivos) sql.append(" and activo=1");
        if (query != null && !query.isBlank()) {
            sql.append(" and (lower(nombre) like :q or lower(coalesce(telefono,'')) like :q or lower(coalesce(documento,'')) like :q)");
            params.put("q", "%" + query.trim().toLowerCase(Locale.ROOT) + "%");
        }
        sql.append(" order by nombre limit 50");
        return jdbc.sql(sql.toString()).params(params).query(this::map).list();
    }

    public Optional<Proveedor> findById(UUID id) {
        return jdbc.sql("select * from proveedor where id=:id").param("id", id.toString()).query(this::map).optional();
    }

    public Optional<Proveedor> findByDocumento(String documento) {
        if (documento == null || documento.isBlank()) return Optional.empty();
        return jdbc.sql("select * from proveedor where documento=:d").param("d", documento.trim()).query(this::map).optional();
    }

    public Proveedor crear(Proveedor p, UUID actor) {
        jdbc.sql("""
                insert into proveedor(id,nombre,telefono,documento,direccion,correo,observaciones,activo,created_by,updated_by)
                values(:id,:nombre,:telefono,:documento,:direccion,:correo,:observaciones,:activo,:actor,:actor)
                """)
                .param("id", p.id().toString()).param("nombre", p.nombre()).param("telefono", p.telefono())
                .param("documento", p.documento()).param("direccion", p.direccion()).param("correo", p.correo())
                .param("observaciones", p.observaciones()).param("activo", p.activo() ? 1 : 0)
                .param("actor", actor.toString()).update();
        return findById(p.id()).orElseThrow();
    }

    public Proveedor actualizar(Proveedor p, UUID actor) {
        int changed = jdbc.sql("""
                update proveedor set nombre=:nombre,telefono=:telefono,documento=:documento,direccion=:direccion,
                    correo=:correo,observaciones=:observaciones,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    updated_by=:actor,version=version+1
                where id=:id and version=:version
                """)
                .param("nombre", p.nombre()).param("telefono", p.telefono()).param("documento", p.documento())
                .param("direccion", p.direccion()).param("correo", p.correo()).param("observaciones", p.observaciones())
                .param("actor", actor.toString()).param("id", p.id().toString()).param("version", p.version()).update();
        if (changed == 0) throw missingOrConflict(p.id());
        return findById(p.id()).orElseThrow();
    }

    public Proveedor cambiarEstado(UUID id, boolean activo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update proveedor set activo=:activo,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    updated_by=:actor,version=version+1
                where id=:id and version=:version
                """)
                .param("activo", activo ? 1 : 0).param("actor", actor.toString())
                .param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id);
        return findById(id).orElseThrow();
    }

    private bo.com.ganadero.shared.error.BusinessException missingOrConflict(UUID id) {
        boolean exists = findById(id).isPresent();
        return new bo.com.ganadero.shared.error.BusinessException(
                exists ? bo.com.ganadero.shared.error.ErrorCode.VERSION_CONFLICT
                        : bo.com.ganadero.shared.error.ErrorCode.PROVEEDOR_NOT_FOUND);
    }

    private Proveedor map(ResultSet r, int row) throws SQLException {
        return new Proveedor(Rows.uuid(r, "id"), r.getString("nombre"), r.getString("telefono"),
                r.getString("documento"), r.getString("direccion"), r.getString("correo"),
                r.getString("observaciones"), r.getBoolean("activo"), Rows.instant(r, "created_at"),
                Rows.uuid(r, "created_by"), Rows.instant(r, "updated_at"), Rows.uuid(r, "updated_by"),
                r.getLong("version"));
    }
}
