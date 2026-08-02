package bo.com.ganadero.empresas.application;

import bo.com.ganadero.empresas.domain.ConfiguracionEmpresa;
import bo.com.ganadero.empresas.domain.ConfiguracionEmpresaRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.shared.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarConfiguracionUseCase {
    private final ConfiguracionEmpresaRepository repository;
    private final UserContext userContext;

    public ConsultarConfiguracionUseCase(ConfiguracionEmpresaRepository repository, UserContext userContext) {
        this.repository = repository; this.userContext = userContext;
    }

    @Transactional(readOnly = true)
    public ConfiguracionEmpresa execute() {
        CurrentUser user = userContext.requirePermission("CONFIGURACION_EMPRESA_VER");
        return repository.findByEmpresaId(user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPRESA_NOT_FOUND,
                        "La configuración de la empresa no existe."));
    }
}
