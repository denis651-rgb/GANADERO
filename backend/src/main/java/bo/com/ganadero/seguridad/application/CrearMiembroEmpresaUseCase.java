package bo.com.ganadero.seguridad.application;
import bo.com.ganadero.seguridad.domain.*;import bo.com.ganadero.seguridad.infrastructure.SupabaseAuthAdminClient;
import bo.com.ganadero.shared.config.AppProperties;import bo.com.ganadero.shared.error.*;import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher;import org.springframework.jdbc.core.simple.JdbcClient;import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;import java.time.Instant;
@Service public class CrearMiembroEmpresaUseCase{
 private final PerfilUsuarioRepository profiles;private final MiembroEmpresaRepository members;private final UserContext context;
 private final ApplicationEventPublisher events;private final SupabaseAuthAdminClient auth;private final AppProperties properties;
 private final JdbcClient jdbc;private final TransactionTemplate transactions;
 public CrearMiembroEmpresaUseCase(PerfilUsuarioRepository profiles,MiembroEmpresaRepository members,UserContext context,
  ApplicationEventPublisher events,SupabaseAuthAdminClient auth,AppProperties properties,JdbcClient jdbc,TransactionTemplate transactions){
  this.profiles=profiles;this.members=members;this.context=context;this.events=events;this.auth=auth;this.properties=properties;this.jdbc=jdbc;this.transactions=transactions;}
 public MiembroEmpresa execute(CrearMiembroCommand command){
  CurrentUser actor=context.requirePermission("USUARIO_CREAR");if(command.roles().isEmpty())throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,"Debe asignarse al menos un rol.");
  SupabaseAuthAdminClient.AdminUser user=auth.invite(command.email(),properties.frontendUrl()+"/auth/aceptar-invitacion");
  try{return transactions.execute(status->{
   profiles.createIfAbsent(user.id(),command.nombres(),command.apellidos(),command.telefono(),actor.userId());
   jdbc.sql("update seguridad.perfiles_usuario set email=:email where id=:id").param("email",command.email().toLowerCase()).param("id",user.id()).update();
   MiembroEmpresa member=members.create(actor.empresaId(),user.id(),command.cargo(),command.accesoTodasPropiedades(),actor.userId());
   members.replaceRoles(member.id(),actor.empresaId(),command.roles(),member.version(),actor.userId());
   MiembroEmpresa afterRoles=members.findByIdAndEmpresaId(member.id(),actor.empresaId()).orElseThrow();
   if(!command.accesoTodasPropiedades())members.replaceProperties(member.id(),actor.empresaId(),command.propiedades(),afterRoles.version(),actor.userId());
   jdbc.sql("update seguridad.miembros_empresa set estado='INVITADO' where id=:id and empresa_id=:empresa").param("id",member.id()).param("empresa",actor.empresaId()).update();
   events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"INVITAR","MIEMBRO_EMPRESA",member.id(),Instant.now()));
   return members.findByIdAndEmpresaId(member.id(),actor.empresaId()).orElseThrow();});
  }catch(RuntimeException exception){auth.deleteIfCreated(user);throw exception;}
 }
}
