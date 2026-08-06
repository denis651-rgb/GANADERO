package bo.com.ganadero;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MigrationPostgisTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Test void appliesAllMigrationsToEmptyPostgisDatabase() throws Exception {
        Flyway flyway=Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword())
                .locations("classpath:db/migration").load();
        assertThat(flyway.migrate().success).isTrue();
        try(var connection=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
            var statement=connection.prepareStatement("""
                select count(*) from information_schema.tables where (table_schema,table_name) in
                (('core','empresas'),('core','propiedades'),('seguridad','perfiles_usuario'),
                 ('seguridad','miembros_empresa'),('seguridad','bootstrap_ejecuciones'),('auditoria','registros'))
                """)){
            try(var result=statement.executeQuery()){result.next();assertThat(result.getInt(1)).isEqualTo(6);}
        }
    }
}
