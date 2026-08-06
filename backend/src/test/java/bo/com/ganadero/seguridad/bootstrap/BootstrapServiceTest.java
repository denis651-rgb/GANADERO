package bo.com.ganadero.seguridad.bootstrap;
import bo.com.ganadero.seguridad.infrastructure.SupabaseAuthAdminClient;import bo.com.ganadero.shared.config.AppProperties;import bo.com.ganadero.shared.error.*;
import org.junit.jupiter.api.Test;import org.springframework.jdbc.core.simple.JdbcClient;import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;import java.util.List;import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.mock;
import jakarta.validation.Validator;import java.util.Set;import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.when;
class BootstrapServiceTest{
 @Test void hidesEndpointWhenDisabled(){BootstrapService service=service(false,"secret");assertCode(()->service.execute("secret","key",request(),"c"),ErrorCode.BOOTSTRAP_DISABLED);}
 @Test void rejectsInvalidToken(){BootstrapService service=service(true,"secret");assertCode(()->service.execute("other","key",request(),"c"),ErrorCode.BOOTSTRAP_TOKEN_INVALID);}
 @Test void requiresIdempotencyKey(){BootstrapService service=service(true,"secret");assertCode(()->service.execute("secret","",request(),"c"),ErrorCode.IDEMPOTENCY_KEY_REQUIRED);}
 private BootstrapService service(boolean enabled,String token){AppProperties p=new AppProperties(new AppProperties.Bootstrap(enabled,token),new AppProperties.SystemStatus(false),"http://localhost:5173",new AppProperties.Storage("bucket",1024,Duration.ofMinutes(5),List.of("image/png"),List.of("png")));Validator validator=mock(Validator.class);when(validator.validate(any(BootstrapRequest.class))).thenReturn(Set.of());return new BootstrapService(p,mock(SupabaseAuthAdminClient.class),mock(JdbcClient.class),mock(TransactionTemplate.class),validator);}
 private BootstrapRequest request(){return new BootstrapRequest(new BootstrapRequest.Empresa("E","Empresa","Empresa",null,null,"e@example.com",null,null,null),new BootstrapRequest.Propietario("o@example.com","A","B",null,null),new BootstrapRequest.Propiedad("P","Propiedad",null,null,null,null,null,null));}
 private void assertCode(ThrowingCallable callable,ErrorCode code){assertThatThrownBy(callable::call).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.code()).isEqualTo(code));}
 @FunctionalInterface interface ThrowingCallable{void call();}
}
