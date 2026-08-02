package bo.com.ganadero.seguridad.domain;

import java.util.Set;
import java.util.UUID;

public record UsuarioActual(UUID usuarioId, String nombres, String apellidos, UUID empresaId,
        String empresaNombre, Set<String> roles, Set<String> permisos,
        Set<UUID> propiedadesPermitidas) {
    public UsuarioActual {
        roles = Set.copyOf(roles); permisos = Set.copyOf(permisos);
        propiedadesPermitidas = Set.copyOf(propiedadesPermitidas);
    }
}
