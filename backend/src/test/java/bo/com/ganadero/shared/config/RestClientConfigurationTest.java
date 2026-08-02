package bo.com.ganadero.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

class RestClientConfigurationTest {

    @Test
    void providesAnIndependentBuilderForEachClient() {
        try (var context = new AnnotationConfigApplicationContext(RestClientConfiguration.class)) {
            var first = context.getBean(RestClient.Builder.class);
            var second = context.getBean(RestClient.Builder.class);

            assertThat(first).isNotSameAs(second);
        }
    }
}
