package bo.com.ganadero.seguridad.application;

import bo.com.ganadero.seguridad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BloquearMiembroUseCaseTest {
    @Test
    void cannotBlockLastActiveOwner() {
        UUID empresaId=UUID.randomUUID(); UUID memberId=UUID.randomUUID();
        MiembroEmpresaRepository repository=mock(MiembroEmpresaRepository.class);
        when(repository.findByIdAndEmpresaId(memberId,empresaId)).thenReturn(Optional.of(owner(memberId,empresaId)));
        when(repository.countActiveOwners(empresaId)).thenReturn(1L);
        CurrentUser actor=new CurrentUser(UUID.randomUUID(),empresaId,UUID.randomUUID(),Set.of("PROPIETARIO"),Set.of("USUARIO_BLOQUEAR"),Set.of(),true);
        BloquearMiembroUseCase useCase=new BloquearMiembroUseCase(repository,new UserContext(()->actor),event->{});

        assertThatThrownBy(()->useCase.execute(memberId,0)).isInstanceOfSatisfying(BusinessException.class,
                error->assertThat(error.code()).isEqualTo(ErrorCode.LAST_ACTIVE_OWNER));
        verify(repository,never()).changeStatus(any(),any(),any(),anyLong(),any());
    }

    private MiembroEmpresa owner(UUID memberId,UUID empresaId){
        PerfilUsuario profile=new PerfilUsuario(UUID.randomUUID(),"Denis","Guarayo",null,null,true,null,Instant.now(),Instant.now(),0);
        Rol owner=new Rol(UUID.randomUUID(),null,"PROPIETARIO","Propietario",null,true,true,Instant.now(),Instant.now(),0,Set.of());
        return new MiembroEmpresa(memberId,empresaId,profile,"Propietario",EstadoMiembro.ACTIVO,LocalDate.now(),true,
                Instant.now(),null,Instant.now(),null,0,Set.of(owner),Set.of());
    }
}
