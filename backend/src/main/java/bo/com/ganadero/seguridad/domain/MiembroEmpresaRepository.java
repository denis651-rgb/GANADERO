package bo.com.ganadero.seguridad.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MiembroEmpresaRepository {
    Optional<MiembroEmpresa> findByIdAndEmpresaId(UUID miembroId, UUID empresaId);
    List<MiembroEmpresa> findAllByEmpresaId(UUID empresaId);
    MiembroEmpresa create(UUID empresaId, UUID usuarioId, String cargo, boolean accesoTotal, UUID actorId);
    MiembroEmpresa update(UUID miembroId, UUID empresaId, String cargo, Boolean accesoTotal,
                          long version, UUID actorId);
    MiembroEmpresa changeStatus(UUID miembroId, UUID empresaId, EstadoMiembro estado, long version, UUID actorId);
    void replaceRoles(UUID miembroId, UUID empresaId, Set<UUID> roleIds, long version, UUID actorId);
    void replaceProperties(UUID miembroId, UUID empresaId, Set<UUID> propertyIds, long version, UUID actorId);
    long countActiveOwners(UUID empresaId);
    Optional<UsuarioActual> findCurrentUser(UUID usuarioId, UUID empresaId);
    boolean existsActiveByEmail(UUID empresaId, String email);
}
