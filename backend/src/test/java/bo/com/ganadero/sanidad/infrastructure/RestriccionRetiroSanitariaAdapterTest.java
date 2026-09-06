package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.ventas.application.RestriccionRetiroPort;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre contra SQLite real: confirma que el puerto de retiro (sección 24) detecta restricciones
 * calculadas tanto desde el flujo de jornada (aplicacion_sanitaria) como desde tratamiento libre
 * (aplicacion_tratamiento), y que no bloquea cuando el retiro ya venció.
 */
class RestriccionRetiroSanitariaAdapterTest {

    @Test
    void detectaRetiroVigenteDesdeUnaAplicacionDeJornada(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbc(tempDir);
        RestriccionRetiroSanitariaAdapter adapter = new RestriccionRetiroSanitariaAdapter(jdbc);
        UUID animalId = insertarAnimalMinimo(jdbc);
        LocalDate fechaVenta = LocalDate.now();

        jdbc.sql("""
                insert into aplicacion_sanitaria(id,animal_id,fecha_aplicacion,retiro_carne_hasta,idempotency_key,estado,origen_registro)
                values(:id,:a,:f,:rc,:k,'APLICADO','APLICADA_FINCA')
                """).param("id", UUID.randomUUID().toString()).param("a", animalId.toString())
                .param("f", fechaVenta.toString()).param("rc", fechaVenta.plusDays(3).toString())
                .param("k", UUID.randomUUID().toString()).update();

        Optional<RestriccionRetiroPort.RestriccionRetiroVigente> resultado = adapter.vigente(UUID.randomUUID(), animalId, fechaVenta);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().tipo()).isEqualTo("CARNE");
    }

    @Test
    void noBloqueaCuandoElRetiroYaVencio(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbc(tempDir);
        RestriccionRetiroSanitariaAdapter adapter = new RestriccionRetiroSanitariaAdapter(jdbc);
        UUID animalId = insertarAnimalMinimo(jdbc);
        LocalDate fechaVenta = LocalDate.now();

        jdbc.sql("""
                insert into aplicacion_sanitaria(id,animal_id,fecha_aplicacion,retiro_carne_hasta,idempotency_key,estado,origen_registro)
                values(:id,:a,:f,:rc,:k,'APLICADO','APLICADA_FINCA')
                """).param("id", UUID.randomUUID().toString()).param("a", animalId.toString())
                .param("f", fechaVenta.minusDays(30).toString()).param("rc", fechaVenta.minusDays(10).toString())
                .param("k", UUID.randomUUID().toString()).update();

        assertThat(adapter.vigente(UUID.randomUUID(), animalId, fechaVenta)).isEmpty();
    }

    @Test
    void detectaRetiroVigenteDesdeUnTratamientoLibre(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbc(tempDir);
        RestriccionRetiroSanitariaAdapter adapter = new RestriccionRetiroSanitariaAdapter(jdbc);
        UUID animalId = insertarAnimalMinimo(jdbc);
        LocalDate fechaVenta = LocalDate.now();

        UUID tratamientoId = UUID.randomUUID();
        jdbc.sql("""
                insert into tratamiento(id,animal_id,fecha_inicio,fecha_fin_estimada,estado)
                values(:id,:a,:f,:ffe,'ACTIVO')
                """).param("id", tratamientoId.toString()).param("a", animalId.toString())
                .param("f", fechaVenta.minusDays(5) + "T00:00:00Z").param("ffe", fechaVenta.plusDays(5) + "T00:00:00Z").update();
        UUID detalleId = UUID.randomUUID();
        jdbc.sql("""
                insert into tratamiento_detalle(id,tratamiento_id,dosis,unidad_dosis,frecuencia_horas,duracion_dias,retiro_leche_dias)
                values(:id,:t,5,'ml',24,5,7)
                """).param("id", detalleId.toString()).param("t", tratamientoId.toString()).update();
        jdbc.sql("""
                insert into aplicacion_tratamiento(id,tratamiento_detalle_id,fecha_programada,dosis_programada,estado,retiro_leche_hasta)
                values(:id,:d,:f,5,'APLICADA',:rl)
                """).param("id", UUID.randomUUID().toString()).param("d", detalleId.toString())
                .param("f", fechaVenta + "T00:00:00Z").param("rl", fechaVenta.plusDays(4).toString()).update();

        Optional<RestriccionRetiroPort.RestriccionRetiroVigente> resultado = adapter.vigente(UUID.randomUUID(), animalId, fechaVenta);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().tipo()).isEqualTo("LECHE");
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

    private JdbcClient jdbc(Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        return JdbcClient.create(dataSource);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("retiro-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
