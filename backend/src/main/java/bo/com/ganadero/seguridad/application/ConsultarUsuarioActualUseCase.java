package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.MiembroEmpresaRepository;
import bo.com.ganadero.seguridad.domain.UsuarioActual;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarUsuarioActualUseCase {
    private final MiembroEmpresaRepository repository; private final UserContext context;
    public ConsultarUsuarioActualUseCase(MiembroEmpresaRepository repository, UserContext context) {
        this.repository=repository; this.context=context;
    }
    @Transactional(readOnly=true) public UsuarioActual execute() {
        CurrentUser user=context.currentUser();
        return repository.findCurrentUser(user.userId(),user.empresaId())
                .orElseThrow(()->new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }
}
