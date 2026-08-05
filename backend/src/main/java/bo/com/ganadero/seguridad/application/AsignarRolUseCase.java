package bo.com.ganadero.seguridad.application;
import bo.com.ganadero.seguridad.domain.*;import bo.com.ganadero.shared.error.*;import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;import java.util.*;
@Service public class AsignarRolUseCase {
 private final MiembroEmpresaRepository members;private final RolRepository roles;private final UserContext context;private final ApplicationEventPublisher events;
 public AsignarRolUseCase(MiembroEmpresaRepository members,RolRepository roles,UserContext context,ApplicationEventPublisher events){this.members=members;this.roles=roles;this.context=context;this.events=events;}
 @Transactional public MiembroEmpresa execute(UUID id,Set<UUID> roleIds,long version){CurrentUser actor=context.requirePermission("USUARIO_ASIGNAR_ROL");
  if(roleIds.isEmpty())throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,"Debe asignarse al menos un rol.");
  MiembroEmpresa member=members.findByIdAndEmpresaId(id,actor.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
  boolean willOwn=roleIds.stream().map(r->roles.findAvailableById(r,actor.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.ROLE_NOT_FOUND))).anyMatch(r->r.codigo().equals("PROPIETARIO"));
  if(member.isOwner()&&!willOwn&&member.perfil().id().equals(actor.userId())) throw new BusinessException(ErrorCode.CANNOT_REMOVE_OWN_OWNER_ROLE);
  if(member.estado()==EstadoMiembro.ACTIVO&&member.isOwner()&&!willOwn&&members.countActiveOwners(actor.empresaId())<=1)throw new BusinessException(ErrorCode.LAST_ACTIVE_OWNER);
  members.replaceRoles(id,actor.empresaId(),roleIds,version,actor.userId());events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"ASIGNAR_ROL","MIEMBRO_EMPRESA",id,Instant.now()));
  return members.findByIdAndEmpresaId(id,actor.empresaId()).orElseThrow();}
}
