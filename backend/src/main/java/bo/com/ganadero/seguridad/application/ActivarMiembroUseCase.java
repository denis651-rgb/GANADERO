package bo.com.ganadero.seguridad.application;
import bo.com.ganadero.seguridad.domain.*; import bo.com.ganadero.shared.security.*; import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.Instant; import java.util.UUID;
@Service public class ActivarMiembroUseCase {
 private final MiembroEmpresaRepository repository;private final UserContext context;private final ApplicationEventPublisher events;
 public ActivarMiembroUseCase(MiembroEmpresaRepository repository,UserContext context,ApplicationEventPublisher events){this.repository=repository;this.context=context;this.events=events;}
 @Transactional public MiembroEmpresa execute(UUID id,long version){CurrentUser actor=context.requirePermission("USUARIO_BLOQUEAR");
  MiembroEmpresa saved=repository.changeStatus(id,actor.empresaId(),EstadoMiembro.ACTIVO,version,actor.userId());events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"ACTIVAR","MIEMBRO_EMPRESA",id,Instant.now()));return saved;}
}
