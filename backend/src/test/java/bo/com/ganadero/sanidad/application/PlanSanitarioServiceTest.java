package bo.com.ganadero.sanidad.application;
import bo.com.ganadero.sanidad.domain.*; import bo.com.ganadero.shared.security.*; import org.junit.jupiter.api.*; import org.springframework.beans.factory.ObjectProvider; import org.springframework.context.ApplicationEventPublisher;
import java.time.*; import java.util.*; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
class PlanSanitarioServiceTest {
 private SanidadRepository repo; private PlanSanitarioService service; private UUID empresa,plan,item;
 @BeforeEach void setUp(){repo=mock(SanidadRepository.class);empresa=UUID.randomUUID();plan=UUID.randomUUID();item=UUID.randomUUID();
  CurrentUser u=new CurrentUser(UUID.randomUUID(),empresa,UUID.randomUUID(),Set.of(),Set.of("SANIDAD_VER","SANIDAD_PLAN_ADMINISTRAR"),Set.of(),true);
  service=new PlanSanitarioService(repo,new UserContext(()->u),mock(ObjectProvider.class),mock(ApplicationEventPublisher.class));}
 @Test void calculaProximaAplicacionYAlertaEnSpring(){when(repo.plan(plan,empresa)).thenReturn(Optional.of(new PlanSanitario(plan,empresa,"Plan",null,LocalDate.now(),null,EstadoPlanSanitario.ACTIVO,null,null,0)));
  when(repo.items(plan,empresa,false)).thenReturn(List.of(new PlanSanitarioItem(item,empresa,plan,TipoActividadSanitaria.VACUNACION,null,"Vacuna",null,null,null,null,null,null,180,7,null,true,true,0)));
  ProximaActividadSanitaria r=service.calcularProxima(plan,item,LocalDate.of(2026,8,13));
  assertThat(r.proximaAplicacion()).isEqualTo(LocalDate.of(2027,2,9));assertThat(r.fechaAlerta()).isEqualTo(LocalDate.of(2027,2,2));}
 @Test void nuevoPlanSiempreNaceEnBorrador(){when(repo.crearPlan(any(),any())).thenAnswer(i->i.getArgument(0));
  PlanSanitario p=service.crearPlan(new CrearPlanSanitarioCommand("Bovinos",null,LocalDate.now(),null));assertThat(p.estado()).isEqualTo(EstadoPlanSanitario.BORRADOR);}
}
