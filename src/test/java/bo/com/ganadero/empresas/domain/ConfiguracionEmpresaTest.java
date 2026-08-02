package bo.com.ganadero.empresas.domain;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracionEmpresaTest {
    @Test
    void rejectsImageQualityOutsideAllowedRange() {
        ConfiguracionEmpresa configuration = configuration();
        assertThatThrownBy(() -> configuration.update(null, null, null, null, null, null,
                null, null, null, 101, 0, UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));
    }

    @Test
    void updatesConfigurationWithTheAuthenticatedActor() {
        ConfiguracionEmpresa configuration = configuration();
        UUID actor = UUID.randomUUID();
        configuration.update(null, null, "USD", 20, null, null, null, null, false, 90, 0, actor);
        assertThat(configuration.moneda()).isEqualTo("USD");
        assertThat(configuration.diasAlertaPreparto()).isEqualTo(20);
        assertThat(configuration.updatedBy()).isEqualTo(actor);
    }

    private ConfiguracionEmpresa configuration() {
        return new ConfiguracionEmpresa(UUID.randomUUID(), UnidadPeso.KG, UnidadSuperficie.HA, "BOB",
                15, 7, 30, false, false, true, 80, Instant.now(), null, Instant.now(), null, 0);
    }
}
