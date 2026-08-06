package bo.com.ganadero.shared.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID userId, UUID empresaId, UUID miembroEmpresaId,
                          Set<String> roles, Set<String> permisos,
                          Set<UUID> propiedadesPermitidas, boolean accesoTodasPropiedades) {
    public CurrentUser {
        roles = Set.copyOf(roles);
        permisos = Set.copyOf(permisos);
        propiedadesPermitidas = Set.copyOf(propiedadesPermitidas);
    }

    public boolean hasPermission(String permiso) { return permisos.contains(permiso); }
}
