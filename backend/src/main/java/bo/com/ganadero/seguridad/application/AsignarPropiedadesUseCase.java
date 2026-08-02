package bo.com.ganadero.seguridad.application;
import bo.com.ganadero.seguridad.domain.*;import bo.com.ganadero.shared.security.*;import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.time.Instant;import java.util.*;
@Service public class AsignarPropiedadesUseCase {
 private final MiembroEmpresaRepository repository;private final UserContext context;private final ApplicationEventPublisher events;
 public AsignarPropiedadesUseCase(MiembroEmpresaRepository repository,UserContext context,ApplicationEventPublisher events){this.repository=repository;this.context=context;this.events=events;}
 @Transactional public MiembroEmpresa execute(UUID id,Set<UUID> propertyIds,long version){CurrentUser actor=context.requirePermission("USUARIO_EDITAR");
  repository.replaceProperties(id,actor.empresaId(),propertyIds,version,actor.userId());events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"ASIGNAR_PROPIEDADES","MIEMBRO_EMPRESA",id,Instant.now()));
  return repository.findByIdAndEmpresaId(id,actor.empresaId()).orElseThrow();}
}
