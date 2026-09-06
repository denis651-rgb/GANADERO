package bo.com.ganadero.sanidad.domain;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadConfigJsonTest {

    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

    @Test
    void deduceConfiguracionPeriodicaSinDiscriminadorAdicional() {
        ModalidadConfig config = json.readValue("""
                {
                  "frecuenciaValor": 30,
                  "frecuenciaUnidad": "DIAS",
                  "referenciaCalculo": "ULTIMA_APLICACION",
                  "toleranciaAnticipadaDias": 2,
                  "toleranciaPosteriorDias": 3
                }
                """, ModalidadConfig.class);

        assertThat(config).isInstanceOf(ModalidadConfig.PeriodicaConfig.class);
        assertThat(((ModalidadConfig.PeriodicaConfig) config).frecuenciaValor()).isEqualTo(30);
    }

    @Test
    void deduceConfiguracionManualVacia() {
        assertThat(json.readValue("{}", ModalidadConfig.class))
                .isInstanceOf(ModalidadConfig.ManualConfig.class);
    }
}
