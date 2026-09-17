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

    // STUB-PERMISOS: app de escritorio de un solo usuario local, acceso total siempre, no hay
    // matriz de permisos. Simetrico con can() en AuthContext.tsx en el frontend (misma
    // decision). Si se agrega multiusuario, esta funcion debe empezar a verificar permisos reales.
    public boolean hasPermission(String permiso) { return true; }
}
