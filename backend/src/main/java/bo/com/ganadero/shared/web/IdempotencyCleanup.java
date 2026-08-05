package bo.com.ganadero.shared.web;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCleanup {
    private final JdbcClient jdbc;

    public IdempotencyCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 600_000L)
    public void cleanup() {
        jdbc.sql("select core.limpiar_idempotencia_expirada(0)").query(Long.class).single();
    }
}
