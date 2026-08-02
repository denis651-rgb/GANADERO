package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.*;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.UUID;

@Service public class ActualizarMiembroEmpresaUseCase {
    private final PerfilUsuarioRepository profiles; private final MiembroEmpresaRepository members;
    private final UserContext context; private final ApplicationEventPublisher events;
    public ActualizarMiembroEmpresaUseCase(PerfilUsuarioRepository profiles,MiembroEmpresaRepository members,UserContext context,ApplicationEventPublisher events){this.profiles=profiles;this.members=members;this.context=context;this.events=events;}
    @Transactional public MiembroEmpresa execute(UUID id,ActualizarMiembroCommand command){
        CurrentUser actor=context.requirePermission("USUARIO_EDITAR");
        MiembroEmpresa member=members.findByIdAndEmpresaId(id,actor.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        profiles.update(member.perfil().id(),command.nombres(),command.apellidos(),command.telefono(),command.avatarPath(),command.perfilVersion(),actor.userId());
        MiembroEmpresa saved=members.update(id,actor.empresaId(),command.cargo(),command.accesoTodasPropiedades(),command.miembroVersion(),actor.userId());
        events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"ACTUALIZAR","MIEMBRO_EMPRESA",id,Instant.now())); return saved;
    }
}
