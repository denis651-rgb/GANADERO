package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.MiembroEmpresa;
import bo.com.ganadero.seguridad.domain.MiembroEmpresaRepository;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service public class ListarMiembrosEmpresaUseCase {
    private final MiembroEmpresaRepository repository; private final UserContext context;
    public ListarMiembrosEmpresaUseCase(MiembroEmpresaRepository repository,UserContext context){this.repository=repository;this.context=context;}
    @Transactional(readOnly=true) public List<MiembroEmpresa> execute(){
        CurrentUser user=context.requirePermission("USUARIO_VER"); return repository.findAllByEmpresaId(user.empresaId());
    }
}
