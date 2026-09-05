package bo.com.ganadero.shared.codigos;

import bo.com.ganadero.shared.security.CurrentUser;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre contra SQLite real: el contador en secuencia_codigo ya aísla por ambito_id (propiedad),
 * pero antes de esta prueba el TEXTO del código de POTRERO/SECTOR ignoraba esa propiedad
 * (prefijo fijo "FINCA"), así que dos propiedades distintas generaban el mismo código y chocaban
 * contra la restricción UNIQUE global de potrero.codigo.
 */
class CodigoServiceSqliteTest {
    @Test
    void generaCodigosDistintosParaPotrerosDeDistintasPropiedades(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        CodigoService service = new CodigoService(jdbc);
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);

        UUID propiedadA = UUID.randomUUID();
        UUID propiedadB = UUID.randomUUID();
        insertarPropiedad(jdbc, propiedadA, "PRP-100");
        insertarPropiedad(jdbc, propiedadB, "PRP-200");

        String codigoA = service.paraCreacion(user, TipoCodigo.POTRERO, propiedadA, null, null);
        String codigoB = service.paraCreacion(user, TipoCodigo.POTRERO, propiedadB, null, null);

        assertThat(codigoA).isEqualTo("PRP-100-POT-001");
        assertThat(codigoB).isEqualTo("PRP-200-POT-001");
        assertThat(codigoA).isNotEqualTo(codigoB);
    }

    private void insertarPropiedad(JdbcClient jdbc, UUID id, String codigo) {
        jdbc.sql("insert into propiedad (id, codigo, nombre) values (:id, :codigo, :nombre)")
                .param("id", id.toString()).param("codigo", codigo).param("nombre", "Propiedad " + codigo)
                .update();
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("codigos-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
