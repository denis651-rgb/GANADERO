package bo.com.ganadero.seguridad.domain;

import java.time.Instant;
import java.util.UUID;

public record PerfilUsuario(UUID id, String nombres, String apellidos, String telefono,
                            String avatarPath, boolean activo, Instant ultimoAccesoAt,
                            Instant createdAt, Instant updatedAt, long version) {}
