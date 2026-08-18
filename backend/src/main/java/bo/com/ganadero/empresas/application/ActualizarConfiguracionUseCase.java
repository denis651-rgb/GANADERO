package bo.com.ganadero.empresas.application;

import bo.com.ganadero.empresas.domain.ConfiguracionEmpresa;
import bo.com.ganadero.empresas.domain.ConfiguracionEmpresaRepository;
import bo.com.ganadero.shared.audit.EmpresaAuditEvent;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ActualizarConfiguracionUseCase {
    private final ConfiguracionEmpresaRepository repository;
    private final UserContext userContext;
    private final ApplicationEventPublisher events;

    public ActualizarConfiguracionUseCase(ConfiguracionEmpresaRepository repository, UserContext userContext,
                                          ApplicationEventPublisher events) {
        this.repository = repository; this.userContext = userContext; this.events = events;
    }

    @Transactional
    public ConfiguracionEmpresa execute(ActualizarConfiguracionCommand command) {
        CurrentUser user = userContext.requirePermission("CONFIGURACION_EMPRESA_EDITAR");
        ConfiguracionEmpresa configuration = repository.findByEmpresaId(user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPRESA_NOT_FOUND,
                        "La configuración de la empresa no existe."));
        configuration.update(command.unidadPeso(), command.unidadSuperficie(), command.moneda(),
                command.diasAlertaPreparto(), command.diasAlertaVacunacion(),
                command.diasDiagnosticoPostServicio(), command.diasGestacionEstimada(), command.diasSinPesaje(),
                command.permitirStockNegativo(), command.requiereAprobacionVenta(),
                command.comprimirImagenes(), command.calidadImagen(), command.version(), user.userId());
        ConfiguracionEmpresa saved = repository.save(configuration);
        events.publishEvent(new EmpresaAuditEvent(user.empresaId(), user.userId(),
                "CONFIGURACION_EMPRESA", user.empresaId(), Instant.now()));
        return saved;
    }
}
