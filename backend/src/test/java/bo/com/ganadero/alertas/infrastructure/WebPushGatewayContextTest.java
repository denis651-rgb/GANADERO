package bo.com.ganadero.alertas.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushGatewayContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "ganadero.push.enabled=false",
                    "ganadero.push.vapid-public-key=",
                    "ganadero.push.vapid-private-key=",
                    "ganadero.push.subject=mailto:soporte@ganadero.bo",
                    "ganadero.push.ttl-seconds=60",
                    "app.frontend-url=https://ganadero.app"
            );

    @Test
    void springSeleccionaElConstructorDeProduccion() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebPushGateway.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(WebPushGateway.class)
    static class TestConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
