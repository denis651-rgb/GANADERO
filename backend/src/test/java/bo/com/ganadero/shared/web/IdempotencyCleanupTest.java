package bo.com.ganadero.shared.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    void eliminaSoloRegistrosExpiradosEnSqlite() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("idempotency.db"));
        JdbcClient jdbc = JdbcClient.create(dataSource);
        jdbc.sql("""
                create table idempotency_records (
                    idempotency_key text primary key,
                    expires_at text
                )
                """).update();
        jdbc.sql("insert into idempotency_records values ('expirada','2000-01-01T00:00:00Z')").update();
        jdbc.sql("insert into idempotency_records values ('vigente','2999-01-01T00:00:00Z')").update();
        jdbc.sql("insert into idempotency_records values ('sin-vencimiento',null)").update();

        new IdempotencyCleanup(jdbc).cleanup();

        assertThat(jdbc.sql("select idempotency_key from idempotency_records order by idempotency_key")
                .query(String.class).list()).containsExactly("sin-vencimiento", "vigente");
    }
}
