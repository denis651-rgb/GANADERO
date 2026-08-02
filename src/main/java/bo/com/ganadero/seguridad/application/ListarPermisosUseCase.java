package bo.com.ganadero.seguridad.application;
import bo.com.ganadero.seguridad.domain.*;import bo.com.ganadero.shared.security.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service public class ListarPermisosUseCase{private final RolRepository repository;private final UserContext context;public ListarPermisosUseCase(RolRepository repository,UserContext context){this.repository=repository;this.context=context;}
 @Transactional(readOnly=true)public List<Permiso> execute(){context.requirePermission("ROL_VER");return repository.findAllPermissions();}}
