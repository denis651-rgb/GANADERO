package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.movimientolote.domain.RestriccionSanitaria;
import bo.com.ganadero.movimientolote.domain.SeveridadRestriccion;
import bo.com.ganadero.sanidad.domain.CasoClinico;
import bo.com.ganadero.sanidad.domain.EstadoCasoClinico;
import bo.com.ganadero.sanidad.domain.EstadoTratamiento;
import bo.com.ganadero.sanidad.domain.RestriccionMovimiento;
import bo.com.ganadero.sanidad.domain.SeveridadCaso;
import bo.com.ganadero.sanidad.domain.Tratamiento;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre contra SQLite real (Flyway aplicado hasta V26): confirma que el adaptador traduce el
 * campo explícito restriccion_movimiento de caso_clinico/tratamiento, en vez de re-derivarlo de
 * la severidad clínica. El caso "LEVE pero BLOQUEANTE" solo puede pasar si se respeta el campo
 * explícito — con la heurística anterior (severidad->severidad) habría dado INFORMATIVA.
 */
class RestriccionSanitariaTrasladoAdapterTest {

    @Test
    void respetaLaRestriccionExplicitaAunqueContradigaLaSeveridadClinica(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository clinica = new JdbcClinicaRepository(jdbc);
        RestriccionSanitariaTrasladoAdapter adapter = new RestriccionSanitariaTrasladoAdapter(jdbc);
        UUID actor = UUID.randomUUID();
        UUID animalId = insertarAnimalMinimo(jdbc);

        clinica.crearCaso(new CasoClinico(UUID.randomUUID(), null, animalId, Instant.now(), "Cojera leve",
                null, null, SeveridadCaso.LEVE, EstadoCasoClinico.ABIERTO, null, null, null, null, 0,
                RestriccionMovimiento.BLOQUEANTE), actor);

        List<RestriccionSanitaria> restricciones = adapter.evaluar(UUID.randomUUID(), animalId);

        assertThat(restricciones).hasSize(1);
        assertThat(restricciones.get(0).severidad()).isEqualTo(SeveridadRestriccion.BLOQUEANTE);
    }

    @Test
    void calculaElValorPorDefectoSoloCuandoNoSeEspecificaExplicitamente(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository clinica = new JdbcClinicaRepository(jdbc);
        RestriccionSanitariaTrasladoAdapter adapter = new RestriccionSanitariaTrasladoAdapter(jdbc);
        UUID actor = UUID.randomUUID();
        UUID animalId = insertarAnimalMinimo(jdbc);

        // Sin restriccionMovimiento explícita: el repositorio persiste null y el adaptador cae
        // en INFORMATIVA (el mismo valor por defecto que ClinicaService habría calculado para LEVE).
        clinica.crearCaso(new CasoClinico(UUID.randomUUID(), null, animalId, Instant.now(), "Cojera leve",
                null, null, SeveridadCaso.LEVE, EstadoCasoClinico.ABIERTO, null, null, null, null, 0, null), actor);

        List<RestriccionSanitaria> restricciones = adapter.evaluar(UUID.randomUUID(), animalId);

        assertThat(restricciones).hasSize(1);
        assertThat(restricciones.get(0).severidad()).isEqualTo(SeveridadRestriccion.INFORMATIVA);
    }

    @Test
    void unTratamientoActivoConRestriccionExplicitaBloqueanteSeRespeta(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository clinica = new JdbcClinicaRepository(jdbc);
        RestriccionSanitariaTrasladoAdapter adapter = new RestriccionSanitariaTrasladoAdapter(jdbc);
        UUID actor = UUID.randomUUID();
        UUID animalId = insertarAnimalMinimo(jdbc);

        Tratamiento creado = clinica.crearTratamiento(new Tratamiento(UUID.randomUUID(), null, null, animalId,
                Instant.now(), Instant.now().plusSeconds(86400), null, null, null, EstadoTratamiento.BORRADOR,
                null, 0, RestriccionMovimiento.BLOQUEANTE), actor);
        jdbc.sql("update tratamiento set estado='ACTIVO' where id=:id").param("id", creado.id().toString()).update();

        List<RestriccionSanitaria> restricciones = adapter.evaluar(UUID.randomUUID(), animalId);

        assertThat(restricciones).hasSize(1);
        assertThat(restricciones.get(0).severidad()).isEqualTo(SeveridadRestriccion.BLOQUEANTE);
    }

    private UUID insertarAnimalMinimo(JdbcClient jdbc) {
        UUID propiedadId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID potreroId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        jdbc.sql("insert into potrero (id, codigo, nombre, propiedad_id) values (:id,:c,:n,:p)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("n", "Potrero test")
                .param("p", propiedadId.toString()).update();
        jdbc.sql("""
                insert into animal (id, codigo, sexo, raza_principal_id, categoria_actual_id, proposito, origen,
                    potrero_actual_id, fecha_ingreso)
                values (:id,:c,'HEMBRA','50000000-0000-0000-0000-000000000001',
                    '60000000-0000-0000-0000-000000000001','CARNE','NACIDO',:pot,:f)
                """).param("id", animalId.toString()).param("c", "ANI-" + animalId)
                .param("pot", potreroId.toString()).param("f", LocalDate.now().toString()).update();
        return animalId;
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("restriccion-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
