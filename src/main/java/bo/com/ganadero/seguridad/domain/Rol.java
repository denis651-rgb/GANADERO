package bo.com.ganadero.seguridad.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Rol(UUID id, UUID empresaId, String codigo, String nombre, String descripcion,
                  boolean sistema, boolean activo, Instant createdAt, Instant updatedAt,
                  long version, Set<Permiso> permisos) {
    public Rol { permisos = Set.copyOf(permisos); }
}
