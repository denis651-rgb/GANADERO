package bo.com.ganadero.seguridad.infrastructure;

import java.util.UUID;

public interface SupabaseAuthAdminClient {
    AdminUser invite(String email, String redirectTo);
    void deleteIfCreated(AdminUser user);
    record AdminUser(UUID id, boolean createdByOperation) {}
}
