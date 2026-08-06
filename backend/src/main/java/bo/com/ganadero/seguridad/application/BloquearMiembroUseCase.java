package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.*; import bo.com.ganadero.shared.error.*; import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.UUID;

@Service public class BloquearMiembroUseCase {
    private final MiembroEmpresaRepository repository; private final UserContext context; private final ApplicationEventPublisher events;
    public BloquearMiembroUseCase(MiembroEmpresaRepository repository,UserContext context,ApplicationEventPublisher events){this.repository=repository;this.context=context;this.events=events;}
    @Transactional public MiembroEmpresa execute(UUID id,long version){CurrentUser actor=context.requirePermission("USUARIO_BLOQUEAR");
        MiembroEmpresa member=repository.findByIdAndEmpresaId(id,actor.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        if(member.perfil().id().equals(actor.userId())) throw new BusinessException(ErrorCode.CANNOT_SELF_BLOCK);
        if(member.estado()==EstadoMiembro.ACTIVO&&member.isOwner()&&repository.countActiveOwners(actor.empresaId())<=1) throw new BusinessException(ErrorCode.LAST_ACTIVE_OWNER);
        MiembroEmpresa saved=repository.changeStatus(id,actor.empresaId(),EstadoMiembro.BLOQUEADO,version,actor.userId());
        events.publishEvent(new SeguridadAuditEvent(actor.empresaId(),actor.userId(),"BLOQUEAR","MIEMBRO_EMPRESA",id,Instant.now()));return saved;}
}
