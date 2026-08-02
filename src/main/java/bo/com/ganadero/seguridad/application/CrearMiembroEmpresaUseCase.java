package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.*;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service public class CrearMiembroEmpresaUseCase {
    private final PerfilUsuarioRepository profiles; private final MiembroEmpresaRepository members;
    private final UserContext context; private final ApplicationEventPublisher events;
    public CrearMiembroEmpresaUseCase(PerfilUsuarioRepository profiles,MiembroEmpresaRepository members,
            UserContext context,ApplicationEventPublisher events){this.profiles=profiles;this.members=members;this.context=context;this.events=events;}
    @Transactional public MiembroEmpresa execute(CrearMiembroCommand command){
        CurrentUser actor=context.requirePermission("USUARIO_CREAR");
        if(command.roles().isEmpty()) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,"Debe asignarse al menos un rol.");
        profiles.createIfAbsent(command.usuarioId(),command.nombres(),command.apellidos(),command.telefono(),actor.userId());
        MiembroEmpresa member=members.create(actor.empresaId(),command.usuarioId(),command.cargo(),command.accesoTodasPropiedades(),actor.userId());
        members.replaceRoles(member.id(),actor.empresaId(),command.roles(),member.version(),actor.userId());
        events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"CREAR","MIEMBRO_EMPRESA",member.id(),Instant.now()));
        return members.findByIdAndEmpresaId(member.id(),actor.empresaId()).orElseThrow();
    }
}
