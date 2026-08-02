package bo.com.ganadero.seguridad.domain;

import java.util.Optional;
import java.util.UUID;

public interface PerfilUsuarioRepository {
    Optional<PerfilUsuario> findById(UUID usuarioId);
    PerfilUsuario createIfAbsent(UUID usuarioId, String nombres, String apellidos, String telefono, UUID actorId);
    PerfilUsuario update(UUID usuarioId, String nombres, String apellidos, String telefono,
                         String avatarPath, long version, UUID actorId);
}
