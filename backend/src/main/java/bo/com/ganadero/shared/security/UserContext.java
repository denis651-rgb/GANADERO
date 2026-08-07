package bo.com.ganadero.shared.security;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserContext {
    private final CurrentUserProvider currentUserProvider;

    public UserContext(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public CurrentUser currentUser() { return currentUserProvider.get(); }
    public UUID empresaId() { return currentUser().empresaId(); }

    public CurrentUser requirePermission(String permiso) {
        CurrentUser user = currentUser();
        if (!user.hasPermission(permiso)) {
            throw new BusinessException(ErrorCode.USER_NOT_AUTHORIZED);
        }
        return user;
    }

    public void requirePropertyAccess(CurrentUser user, UUID propiedadId) {
        if (!user.accesoTodasPropiedades() && !user.propiedadesPermitidas().contains(propiedadId)) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }
    }

    public boolean hasPropertyAccess(CurrentUser user, UUID propiedadId) {
        return user.accesoTodasPropiedades() || user.propiedadesPermitidas().contains(propiedadId);
    }
}
