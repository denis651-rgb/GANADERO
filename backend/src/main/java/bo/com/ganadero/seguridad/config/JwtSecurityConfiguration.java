package bo.com.ganadero.seguridad.config;

import bo.com.ganadero.shared.web.RestAccessDeniedHandler;
import bo.com.ganadero.shared.web.RestAuthenticationEntryPoint;
import bo.com.ganadero.shared.web.IdempotencyFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true")
class JwtSecurityConfiguration {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final IdempotencyFilter idempotencyFilter;

    JwtSecurityConfiguration(RestAuthenticationEntryPoint authenticationEntryPoint,
                             RestAccessDeniedHandler accessDeniedHandler,
                             IdempotencyFilter idempotencyFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.idempotencyFilter = idempotencyFilter;
    }

    @Bean
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/bootstrap/empresa-inicial").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/internal/jobs/alertas/activar",
                                "/api/internal/jobs/alertas/pesajes/generar",
                                "/api/internal/jobs/alertas/vacunacion/generar",
                                "/api/internal/jobs/alertas/tratamientos/vencidos",
                                "/api/internal/jobs/alertas/recordatorios/procesar",
                                "/api/internal/jobs/notificaciones/procesar").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterAfter(idempotencyFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<IdempotencyFilter> disableAutomaticIdempotencyFilterRegistration() {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>(idempotencyFilter);
        registration.setEnabled(false);
        return registration;
    }
}
