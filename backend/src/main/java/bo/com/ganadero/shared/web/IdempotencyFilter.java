package bo.com.ganadero.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final String HEADER = "Idempotency-Key";
    private final JdbcClient jdbc;

    public IdempotencyFilter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return request.getHeader(HEADER) == null
                || !(method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER).trim();
        if (key.isEmpty() || key.length() > 200) {
            writeJson(response, 400, "{\"success\":false,\"code\":\"INVALID_IDEMPOTENCY_KEY\",\"message\":\"Idempotency-Key debe contener entre 1 y 200 caracteres.\"}");
            return;
        }

        String subject = authenticatedSubject();
        String method = request.getMethod();
        String path = request.getRequestURI();
        int reserved = jdbc.sql("""
                insert into core.idempotency_records(subject,idempotency_key,http_method,request_path)
                values(:subject,:key,:method,:path) on conflict do nothing
                """).param("subject", subject).param("key", key).param("method", method).param("path", path).update();

        if (reserved == 0) {
            Optional<StoredResponse> stored = find(subject, key, method, path);
            if (stored.isPresent() && stored.get().completed()) {
                StoredResponse value = stored.get();
                response.setStatus(value.status());
                if (value.contentType() != null) response.setContentType(value.contentType());
                response.getOutputStream().write(value.body());
            } else {
                writeJson(response, 409, "{\"success\":false,\"code\":\"REQUEST_IN_PROGRESS\",\"message\":\"Ya existe una operación en curso con esta clave.\"}");
            }
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapped);
            byte[] body = wrapped.getContentAsByteArray();
            if (wrapped.getStatus() >= 500) {
                remove(subject, key, method, path);
            } else {
                jdbc.sql("""
                        update core.idempotency_records set state='COMPLETED',response_status=:status,
                        response_content_type=:contentType,response_body=:body,completed_at=now()
                        where subject=:subject and idempotency_key=:key and http_method=:method and request_path=:path
                        """).param("status", wrapped.getStatus()).param("contentType", wrapped.getContentType())
                        .param("body", body).param("subject", subject).param("key", key)
                        .param("method", method).param("path", path).update();
            }
        } catch (IOException | ServletException | RuntimeException exception) {
            remove(subject, key, method, path);
            throw exception;
        } finally {
            wrapped.copyBodyToResponse();
        }
    }

    private String authenticatedSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) return jwt.getSubject();
        return "anonymous";
    }

    private Optional<StoredResponse> find(String subject, String key, String method, String path) {
        return jdbc.sql("""
                select state,response_status,response_content_type,response_body from core.idempotency_records
                where subject=:subject and idempotency_key=:key and http_method=:method and request_path=:path
                """).param("subject", subject).param("key", key).param("method", method).param("path", path)
                .query(this::map).optional();
    }

    private StoredResponse map(ResultSet result, int row) throws SQLException {
        return new StoredResponse("COMPLETED".equals(result.getString("state")), result.getInt("response_status"),
                result.getString("response_content_type"), result.getBytes("response_body"));
    }

    private void remove(String subject, String key, String method, String path) {
        jdbc.sql("delete from core.idempotency_records where subject=:subject and idempotency_key=:key and http_method=:method and request_path=:path")
                .param("subject", subject).param("key", key).param("method", method).param("path", path).update();
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }

    private record StoredResponse(boolean completed, int status, String contentType, byte[] body) {}
}
