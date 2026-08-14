package bo.com.ganadero.shared.codigos;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CodigoServiceTest {
    private final CodigoService service = new CodigoService(mock(JdbcClient.class));

    @Test
    void formatsStableAutomaticCodes() {
        assertThat(service.formatear(TipoCodigo.PROPIEDAD, null, 0, 7)).isEqualTo("PRP-007");
        assertThat(service.formatear(TipoCodigo.ANIMAL, null, 0, 61)).isEqualTo("ANI-000061");
        assertThat(service.formatear(TipoCodigo.LOTE, null, 2026, 12)).isEqualTo("LOT-2026-0012");
    }

    @Test
    void normalizesManualCodes() {
        assertThat(service.normalizarManual("  hacienda  norte  ")).isEqualTo("HACIENDA-NORTE");
    }

    @Test
    void rejectsChangingAnExistingCodeWithoutAdministrativePermission() {
        CurrentUser user = user(Set.of());

        assertThatThrownBy(() -> service.paraActualizacion(user, TipoCodigo.ANIMAL, null, null,
                "ANI-000001", "ANI-000099"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.USER_NOT_AUTHORIZED));
    }

    @Test
    void keepsExistingCodeWhenRequestOmitsIt() {
        assertThat(service.paraActualizacion(user(Set.of()), TipoCodigo.ANIMAL, null, null,
                "ANI-000001", null)).isEqualTo("ANI-000001");
    }

    private CurrentUser user(Set<String> permissions) {
        return new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(), permissions,
                Set.of(), true);
    }
}
