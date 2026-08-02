package bo.com.ganadero.seguridad.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RolRepository {
    List<Rol> findAllAvailableFor(UUID empresaId);
    Optional<Rol> findAvailableById(UUID roleId, UUID empresaId);
    List<Permiso> findAllPermissions();
    Rol create(UUID empresaId, String codigo, String nombre, String descripcion, UUID actorId);
    Rol update(UUID roleId, UUID empresaId, String nombre, String descripcion, Boolean activo,
               long version, UUID actorId);
    Rol replacePermissions(UUID roleId, UUID empresaId, Set<UUID> permissionIds, long version, UUID actorId);
}
