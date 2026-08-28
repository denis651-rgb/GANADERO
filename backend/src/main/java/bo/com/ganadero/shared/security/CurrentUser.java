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

    // App de escritorio de un solo usuario local: acceso total siempre, no hay matriz de permisos.
    public boolean hasPermission(String permiso) { return true; }
}
