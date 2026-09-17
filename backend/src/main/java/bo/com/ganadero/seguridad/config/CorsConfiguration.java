package bo.com.ganadero.seguridad.config;

import bo.com.ganadero.shared.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * App de escritorio: el backend solo escucha en 127.0.0.1 para el proceso local de Electron,
 * asi que en principio ningun origen deberia poder ser malicioso. Aun asi, los origenes se
 * restringen explicitamente a los que la app realmente usa (esquema "app://" del frontend
 * empaquetado + servidor de Vite en dev) via app.cors.allowed-origins en vez de un wildcard,
 * para no quedar permisivos por defecto si algun dia se agrega acceso remoto.
 */
@Configuration
class CorsConfiguration {

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        return source;
    }
}
