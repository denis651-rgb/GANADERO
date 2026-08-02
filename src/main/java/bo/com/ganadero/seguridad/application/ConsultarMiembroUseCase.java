package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.*;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service public class ConsultarMiembroUseCase {
    private final MiembroEmpresaRepository repository; private final UserContext context;
    public ConsultarMiembroUseCase(MiembroEmpresaRepository repository,UserContext context){this.repository=repository;this.context=context;}
    @Transactional(readOnly=true) public MiembroEmpresa execute(UUID id){CurrentUser user=context.requirePermission("USUARIO_VER");
        return repository.findByIdAndEmpresaId(id,user.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));}
}
