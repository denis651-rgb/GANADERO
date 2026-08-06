package bo.com.ganadero.auditoria.application;

import bo.com.ganadero.auditoria.domain.AuditPage;
import bo.com.ganadero.auditoria.domain.AuditoriaFilter;
import bo.com.ganadero.auditoria.domain.AuditoriaRepository;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repository;
    private final UserContext context;

    public AuditoriaService(AuditoriaRepository repository, UserContext context) {
        this.repository = repository;
        this.context = context;
    }

    @Transactional(readOnly = true)
    public AuditPage list(AuditoriaFilter filter) {
        CurrentUser user = context.requirePermission("AUDITORIA_VER");
        return repository.findAll(user.empresaId(), filter);
    }
}
