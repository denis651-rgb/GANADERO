package bo.com.ganadero.empresas.application;

import bo.com.ganadero.empresas.domain.Empresa;
import bo.com.ganadero.empresas.domain.EmpresaRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.shared.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarEmpresaUseCase {
    private final EmpresaRepository repository;
    private final UserContext userContext;

    public ConsultarEmpresaUseCase(EmpresaRepository repository, UserContext userContext) {
        this.repository = repository; this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public Empresa execute() {
        CurrentUser user = userContext.requirePermission("EMPRESA_VER");
        return repository.findById(user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPRESA_NOT_FOUND));
    }
}
