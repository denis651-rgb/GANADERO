package bo.com.ganadero.sync.api;

import java.util.Set;
import java.util.UUID;

public record SyncUsuarioInfo(
        UUID id,
        Set<String> roles,
        Set<String> permisos,
        Set<UUID> propiedadesPermitidas,
        boolean accesoTodasPropiedades) {
}
