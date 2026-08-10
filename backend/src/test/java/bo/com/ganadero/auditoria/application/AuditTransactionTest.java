package bo.com.ganadero.auditoria.application;

import bo.com.ganadero.auditoria.domain.AuditoriaRepository;
import bo.com.ganadero.auditoria.infrastructure.JdbcAuditoriaRepository;
import bo.com.ganadero.shared.audit.EmpresaAuditEvent;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class AuditTransactionTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("ganadero_audit_test")
            .withUsername("ganadero_test")
            .withPassword("ganadero_test");

    static {
        POSTGRES.start();
    }

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    JdbcClient jdbc;

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName(POSTGRES.getDriverClassName());
            ds.setUrl(POSTGRES.getJdbcUrl());
            ds.setUsername(POSTGRES.getUsername());
            ds.setPassword(POSTGRES.getPassword());
            return ds;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcClient jdbcClient(DataSource dataSource) {
            return JdbcClient.create(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        AuditoriaRepository auditoriaRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
            return new JdbcAuditoriaRepository(jdbcClient, objectMapper);
        }

        @Bean
        AuditEventListener auditEventListener(AuditoriaRepository auditoriaRepository) {
            return new AuditEventListener(auditoriaRepository);
        }
    }

    @Test
    void registraAuditoriaCuandoLaTransaccionHaceCommit() {
        UUID empresa = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            insertEmpresa(empresa);
            events.publishEvent(new AuditLogEvent(empresa, usuario, "CREAR", "ANIMALES", "ANIMAL",
                    UUID.randomUUID(), Map.of(), Map.of("codigo", "T-1"), Instant.now()));
        });

        assertThat(countPorEmpresa(empresa)).isEqualTo(1);
    }

    @Test
    void noRegistraAuditoriaCuandoLaTransaccionHaceRollback() {
        UUID empresa = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                insertEmpresa(empresa);
                events.publishEvent(new AuditLogEvent(empresa, usuario, "CREAR", "ANIMALES", "ANIMAL",
                        UUID.randomUUID(), Map.of(), Map.of(), Instant.now()));
                status.setRollbackOnly();
            });
        } catch (UnexpectedRollbackException ignored) {
        }

        assertThat(countPorEmpresa(empresa)).isZero();
    }

    @Test
    void registraEventosDeEmpresaTrasElCommit() {
        UUID empresa = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            insertEmpresa(empresa);
            events.publishEvent(new EmpresaAuditEvent(empresa, usuario, "CONFIGURACION_EMPRESA",
                    empresa, Instant.now()));
        });

        Integer count = jdbc.sql("""
                select count(*) from auditoria.registros
                where empresa_id = :e and modulo = 'EMPRESAS' and accion = 'ACTUALIZAR'
                """).param("e", empresa).query(Integer.class).single();
        assertThat(count).isEqualTo(1);
    }

    private void insertEmpresa(UUID empresa) {
        jdbc.sql("""
                insert into core.empresas
                    (id, codigo, razon_social, nombre_comercial, estado, created_at, updated_at)
                values (:id, :codigo, 'Empresa audit test S.R.L.', 'Empresa audit test', 'ACTIVA', now(), now())
                """)
                .param("id", empresa)
                .param("codigo", "AUD-" + empresa.toString().substring(0, 8))
                .update();
    }

    private Integer countPorEmpresa(UUID empresa) {
        return jdbc.sql("select count(*) from auditoria.registros where empresa_id = :e")
                .param("e", empresa).query(Integer.class).single();
    }
}
