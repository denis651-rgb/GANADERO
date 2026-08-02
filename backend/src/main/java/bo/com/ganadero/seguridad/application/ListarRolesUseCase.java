package bo.com.ganadero.seguridad.application;
import bo.com.ganadero.seguridad.domain.*;import bo.com.ganadero.shared.security.*;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;
@Service public class ListarRolesUseCase{private final RolRepository repository;private final UserContext context;public ListarRolesUseCase(RolRepository repository,UserContext context){this.repository=repository;this.context=context;}
 @Transactional(readOnly=true)public List<Rol> execute(){CurrentUser user=context.requirePermission("ROL_VER");return repository.findAllAvailableFor(user.empresaId());}
 @Transactional(readOnly=true)public Rol get(UUID id){CurrentUser user=context.requirePermission("ROL_VER");return repository.findAvailableById(id,user.empresaId()).orElseThrow(()->new bo.com.ganadero.shared.error.BusinessException(bo.com.ganadero.shared.error.ErrorCode.ROLE_NOT_FOUND));}}
