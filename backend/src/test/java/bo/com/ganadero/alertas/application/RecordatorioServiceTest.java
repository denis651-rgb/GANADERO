package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.*;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecordatorioServiceTest {
    private final UUID empresa=UUID.randomUUID(),usuario=UUID.randomUUID();
    private final RecordatorioRepository repository=mock(RecordatorioRepository.class);
    private final MotorAlertas motor=mock(MotorAlertas.class);
    private final RecordatorioService service=new RecordatorioService(repository,motor,
            new UserContext(()->new CurrentUser(usuario,empresa,UUID.randomUUID(),Set.of(),Set.of("ALERTA_VER","ALERTA_CONFIGURAR"),Set.of(),true)),
            mock(AnimalRepository.class));

    @Test void creaUnaProgramacionValida(){
        Instant first=Instant.now().plusSeconds(3600),event=first.plusSeconds(7200);
        when(repository.guardar(any())).thenAnswer(i->i.getArgument(0));
        Recordatorio result=service.crear(new CrearRecordatorioCommand("Vacunar","Aplicar vacuna",SeveridadAlerta.WARNING,null,event,first,3,60));
        assertThat(result.estado()).isEqualTo(EstadoRecordatorio.ACTIVO);
        assertThat(result.cantidadNotificaciones()).isEqualTo(3);
    }

    @Test void rechazaOcurrenciasPosterioresAlEvento(){
        Instant first=Instant.now().plusSeconds(3600),event=first.plusSeconds(1800);
        assertThatThrownBy(()->service.crear(new CrearRecordatorioCommand("Vacunar","Aplicar",SeveridadAlerta.WARNING,null,event,first,2,60)))
                .isInstanceOf(BusinessException.class);
    }

    @Test void generaUnaOcurrenciaYProgramaLaSiguiente(){
        Instant first=Instant.now().minusSeconds(60),event=Instant.now().plusSeconds(7200);
        Recordatorio r=new Recordatorio(UUID.randomUUID(),empresa,usuario,"Vacunar","Aplicar vacuna",SeveridadAlerta.URGENTE,null,event,first,2,60,0,EstadoRecordatorio.ACTIVO,null,null,0);
        when(repository.bloquearVencidos(any(),eq(100))).thenReturn(List.of(r));
        assertThat(service.procesar()).isEqualTo(1);
        verify(motor).crearInmediata(argThat(c->c.tipo()==TipoAlerta.RECORDATORIO_SANIDAD&&"1".equals(String.valueOf(c.metadata().get("ocurrencia")))));
        verify(repository).registrarEjecucion(eq(r),eq(first.plusSeconds(3600)),eq(false));
    }

    @Test void cancelarCierraLasAlertasGeneradas(){
        UUID id=UUID.randomUUID();Recordatorio r=new Recordatorio(id,empresa,usuario,"x","y",SeveridadAlerta.INFO,null,Instant.now(),Instant.now(),1,null,0,EstadoRecordatorio.ACTIVO,null,null,2);
        when(repository.cambiarEstado(id,empresa,EstadoRecordatorio.CANCELADO,2)).thenReturn(r);
        service.cancelar(id,2);
        verify(repository).cancelarAlertas(id,empresa);
    }
}
