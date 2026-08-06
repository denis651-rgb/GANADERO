package bo.com.ganadero.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final String HEADER = "Idempotency-Key";
    private static final String JSON_ERROR = "\"ok\":false,\"code\":\"%s\",\"message\":\"%s\",\"fieldErrors\":[],\"timestamp\":\"%s\",\"correlationId\":\"%s\"";
    private final JdbcClient jdbc;
    private final long ttlHours;

    public IdempotencyFilter(JdbcClient jdbc, @Value("${app.idempotency.ttl-hours:24}") long ttlHours) {
        this.jdbc = jdbc;
        this.ttlHours = ttlHours;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return request.getRequestURI().equals("/bootstrap/empresa-inicial")
                || request.getHeader(HEADER) == null
                || !(method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER).trim();
        if (key.isEmpty() || key.length() > 200) {
            writeJson(response, 400, errorBody("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key debe contener entre 1 y 200 caracteres.", request));
            return;
        }

        String subject = authenticatedSubject();
        String method = request.getMethod();
        String path = request.getRequestURI();

        byte[] payload = readBody(request);
        String hash = sha256(payload);
        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request, payload);

        int reserved = jdbc.sql("""
                insert into core.idempotency_records(subject,idempotency_key,http_method,request_path,payload_hash,expires_at)
                values(:subject,:key,:method,:path,:hash,now() + (:ttlHours || ' hours')::interval)
                on conflict do nothing
                """).param("subject", subject).param("key", key).param("method", method).param("path", path)
                .param("hash", hash).param("ttlHours", ttlHours).update();

        if (reserved == 0) {
            Optional<StoredResponse> stored = find(subject, key, method, path);
            if (stored.isPresent() && stored.get().completed()) {
                StoredResponse value = stored.get();
                if (value.payloadHash() != null && !value.payloadHash().equals(hash)) {
                    writeJson(response, 409, errorBody("IDEMPOTENCY_CONFLICT",
                            "La clave de idempotencia ya fue usada con otra solicitud.", request));
                    return;
                }
                response.setStatus(value.status());
                if (value.contentType() != null) response.setContentType(value.contentType());
                response.getOutputStream().write(value.body());
            } else {
                writeJson(response, 409, errorBody("REQUEST_IN_PROGRESS",
                        "Ya existe una operación en curso con esta clave.", request));
            }
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(wrappedRequest, wrapped);
            byte[] body = wrapped.getContentAsByteArray();
            if (wrapped.getStatus() >= 500) {
                remove(subject, key, method, path);
            } else {
                jdbc.sql("""
                        update core.idempotency_records set state='COMPLETED',response_status=:status,
                        response_content_type=:contentType,response_body=:body,correlation_id=:correlationId,
                        completed_at=now()
                        where subject=:subject and idempotency_key=:key and http_method=:method and request_path=:path
                        """).param("status", wrapped.getStatus()).param("contentType", wrapped.getContentType())
                        .param("body", body).param("correlationId", correlationId(wrappedRequest))
                        .param("subject", subject).param("key", key)
                        .param("method", method).param("path", path).update();
            }
        } catch (IOException | ServletException | RuntimeException exception) {
            remove(subject, key, method, path);
            throw exception;
        } finally {
            wrapped.copyBodyToResponse();
        }
    }

    private byte[] readBody(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyRequestWrapper cached) return cached.body();
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException exception) {
            return new byte[0];
        }
    }

    private String sha256(byte[] payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? "unknown" : value.toString();
    }

    private String errorBody(String code, String message, HttpServletRequest request) {
        return "{" + JSON_ERROR.formatted(code, message, Instant.now().toString(), correlationId(request)) + "}";
    }

    private String authenticatedSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) return jwt.getSubject();
        return "anonymous";
    }

    private Optional<StoredResponse> find(String subject, String key, String method, String path) {
        return jdbc.sql("""
                select state,response_status,response_content_type,response_body,payload_hash
                from core.idempotency_records
                where subject=:subject and idempotency_key=:key and http_method=:method and request_path=:path
                """).param("subject", subject).param("key", key).param("method", method).param("path", path)
                .query(this::map).optional();
    }

    private StoredResponse map(ResultSet result, int row) throws SQLException {
        return new StoredResponse("COMPLETED".equals(result.getString("state")), result.getInt("response_status"),
                result.getString("response_content_type"), result.getBytes("response_body"),
                result.getString("payload_hash"));
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

    private record StoredResponse(boolean completed, int status, String contentType, byte[] body, String payloadHash) {}
}
