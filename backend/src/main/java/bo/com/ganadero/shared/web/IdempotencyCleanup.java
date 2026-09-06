package bo.com.ganadero.shared.web;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class IdempotencyCleanup {
    private final JdbcClient jdbc;

    public IdempotencyCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 600_000L)
    public void cleanup() {
        jdbc.sql("delete from idempotency_records where expires_at is not null and expires_at < :now")
                .param("now", Instant.now().toString())
                .update();
    }
}
