package bo.com.ganadero.empresas.application;

import bo.com.ganadero.empresas.domain.Empresa;
import bo.com.ganadero.empresas.domain.EmpresaRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ActualizarEmpresaUseCase {
    private final EmpresaRepository repository;
    private final UserContext userContext;
    private final ApplicationEventPublisher events;

    public ActualizarEmpresaUseCase(EmpresaRepository repository, UserContext userContext,
                                    ApplicationEventPublisher events) {
        this.repository = repository; this.userContext = userContext; this.events = events;
    }

    @Transactional
    public Empresa execute(ActualizarEmpresaCommand command) {
        CurrentUser user = userContext.requirePermission("EMPRESA_EDITAR");
        Empresa empresa = repository.findById(user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPRESA_NOT_FOUND));
        empresa.update(command.razonSocial(), command.nombreComercial(), command.nit(), command.telefono(),
                command.email(), command.direccion(), command.version(), user.userId());
        Empresa saved = repository.save(empresa);
        events.publishEvent(new EmpresaActualizadaEvent(user.empresaId(), user.userId(),
                "EMPRESA", empresa.id(), Instant.now()));
        return saved;
    }
}
