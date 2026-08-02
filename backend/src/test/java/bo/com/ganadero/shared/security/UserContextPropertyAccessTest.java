package bo.com.ganadero.shared.security;
import bo.com.ganadero.shared.error.*; import org.junit.jupiter.api.Test; import java.util.*;
import static org.assertj.core.api.Assertions.*;
class UserContextPropertyAccessTest {
 @Test void restrictedUserCannotAccessUnassignedProperty(){UUID assigned=UUID.randomUUID();CurrentUser user=user(false,Set.of(assigned));UserContext context=new UserContext(()->user);assertThatThrownBy(()->context.requirePropertyAccess(user,UUID.randomUUID())).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.code()).isEqualTo(ErrorCode.PROPERTY_ACCESS_DENIED));}
 @Test void unrestrictedUserCanAccessAnyProperty(){CurrentUser user=user(true,Set.of());UserContext context=new UserContext(()->user);assertThatCode(()->context.requirePropertyAccess(user,UUID.randomUUID())).doesNotThrowAnyException();}
 private CurrentUser user(boolean all,Set<UUID> properties){return new CurrentUser(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),Set.of(),Set.of(),properties,all);}
}
