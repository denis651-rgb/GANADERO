package bo.com.ganadero.shared.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * App de escritorio de un solo usuario local: no hay login remoto, multi-empresa
 * ni roles/permisos granulares. Este es el unico CurrentUserProvider; el usuario
 * local tiene acceso total siempre.
 */
@Component
public class LocalCurrentUserProvider implements CurrentUserProvider {
    private static final UUID LOCAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final CurrentUser currentUser = new CurrentUser(
            LOCAL_ID, LOCAL_ID, LOCAL_ID,
            Set.of("PROPIETARIO"), Set.of(), Set.of(), true);

    @Override
    public CurrentUser get() {
        return currentUser;
    }
}
