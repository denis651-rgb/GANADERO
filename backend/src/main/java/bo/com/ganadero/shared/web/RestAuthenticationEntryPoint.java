package bo.com.ganadero.shared.web;

import bo.com.ganadero.shared.api.ApiError;
import bo.com.ganadero.shared.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log =
            LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.error(
                "Authentication failed. method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getMessage(),
                authException
        );

        response.setStatus(ErrorCode.UNAUTHENTICATED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiError(
                        false,
                        ErrorCode.UNAUTHENTICATED.name(),
                        ErrorCode.UNAUTHENTICATED.defaultMessage(),
                        List.of(),
                        Instant.now(),
                        correlationId(request)
                )
        );
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? "unknown" : value.toString();
    }
}