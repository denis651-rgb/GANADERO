package bo.com.ganadero.seguridad.bootstrap;
import bo.com.ganadero.seguridad.infrastructure.SupabaseAuthAdminClient;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test void hidesEndpointWhenDisabled() {
        BootstrapService service = service(false, "secret");
        assertCode(() -> service.execute("secret", "key", request(), "c"), ErrorCode.BOOTSTRAP_DISABLED);
    }

    @Test void rejectsInvalidToken() {
        BootstrapService service = service(true, "secret");
        assertCode(() -> service.execute("other", "key", request(), "c"), ErrorCode.BOOTSTRAP_TOKEN_INVALID);
    }

    @Test void requiresIdempotencyKey() {
        BootstrapService service = service(true, "secret");
        assertCode(() -> service.execute("secret", "", request(), "c"), ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test void samePayloadProducesStableHash() {
        BootstrapService service = service(true, "secret");
        assertThat(service.payloadHash(request()))
                .isEqualTo(service.payloadHash(request()));
    }

    @Test void differentPayloadProducesDifferentHash() {
        BootstrapService service = service(true, "secret");
        assertThat(service.payloadHash(request()))
                .isNotEqualTo(service.payloadHash(request("o2@example.com")));
    }

    @Test void compensatesSupabaseUserAndRethrowsWhenLocalTransactionFails() {
        SupabaseAuthAdminClient auth = mock(SupabaseAuthAdminClient.class);
        when(auth.invite(anyString(), anyString()))
                .thenReturn(new SupabaseAuthAdminClient.AdminUser(UUID.randomUUID(), true));
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenThrow(new IllegalStateException("fallo local"));
        JdbcClient jdbc = fluentJdbcMock();
        BootstrapService service = service(true, "secret", auth, jdbc, transactions);

        assertThatThrownBy(() -> service.execute("secret", "key", request(), "c"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fallo local");

        verify(auth).deleteIfCreated(any(SupabaseAuthAdminClient.AdminUser.class));
        verify(jdbc).sql(argThat(sql -> sql.contains("BOOTSTRAP_INICIAL") && sql.contains("'ERROR'")));
    }

    private BootstrapService service(boolean enabled, String token) {
        return service(enabled, token, mock(SupabaseAuthAdminClient.class),
                fluentJdbcMock(), mock(TransactionTemplate.class));
    }

    private BootstrapService service(boolean enabled, String token, SupabaseAuthAdminClient auth,
            JdbcClient jdbc, TransactionTemplate transactions) {
        AppProperties p = new AppProperties(new AppProperties.Bootstrap(enabled, token),
                new AppProperties.InternalJobs(false, ""),
                new AppProperties.SystemStatus(false), "http://localhost:5173",
                new AppProperties.Storage("bucket", 1024, Duration.ofMinutes(5),
                        List.of("image/png"), List.of("png")));
        Validator validator = mock(Validator.class);
        when(validator.validate(any(BootstrapRequest.class))).thenReturn(Set.of());
        return new BootstrapService(p, auth, jdbc, transactions, validator, objectMapper);
    }

    @SuppressWarnings("unchecked")
    private JdbcClient fluentJdbcMock() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec spec = mock(JdbcClient.StatementSpec.class);
        JdbcClient.MappedQuerySpec<Object> querySpec = mock(JdbcClient.MappedQuerySpec.class);
        when(querySpec.optional()).thenReturn(Optional.empty());
        when(querySpec.single()).thenReturn(0L);
        when(jdbc.sql(anyString())).thenReturn(spec);
        when(spec.param(anyString(), any())).thenReturn(spec);
        when(spec.query(any(RowMapper.class))).thenReturn(querySpec);
        when(spec.query(any(Class.class))).thenReturn(querySpec);
        return jdbc;
    }

    private BootstrapRequest request() {
        return request("o@example.com");
    }

    private BootstrapRequest request(String ownerEmail) {
        return new BootstrapRequest(
                new BootstrapRequest.Empresa("E", "Empresa", "Empresa", null, null,
                        "e@example.com", null, null, null),
                new BootstrapRequest.Propietario(ownerEmail, "A", "B", null, null),
                new BootstrapRequest.Propiedad("P", "Propiedad", null, null, null, null, null, null));
    }

    private void assertCode(ThrowingCallable callable, ErrorCode code) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(code));
    }

    @FunctionalInterface
    interface ThrowingCallable { void call(); }
}
