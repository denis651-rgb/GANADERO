package bo.com.ganadero.timeline.infrastructure;

import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regresión de la migración V42: el CHECK {@code ck_evento_animal_tipo} debe
 * aceptar todos los tipos de {@link TipoEventoAnimal}. Los eventos de los
 * controles sanitarios individuales (control neonatal, control ectoparasitario y
 * examen reproductivo) quedaron fuera del baseline V1 y rompían el guardado con
 * SQLITE_CONSTRAINT_CHECK. Se recorre el enum completo para que cualquier tipo
 * nuevo que se agregue sin migración haga fallar esta prueba.
 */
class EventoTimelineTipoCheckTest {

    @Test
    void elCheckAceptaTodosLosTiposDelEnum(@TempDir Path tempDir) {
        JdbcClient jdbc = migrar(tempDir);
        UUID animalId = sembrarAnimal(jdbc);

        for (TipoEventoAnimal tipo : TipoEventoAnimal.values()) {
            jdbc.sql("insert into evento_animal(id,animal_id,tipo) values(:id,:animal,:tipo)")
                    .param("id", UUID.randomUUID().toString())
                    .param("animal", animalId.toString())
                    .param("tipo", tipo.name())
                    .update();
        }

        long total = jdbc.sql("select count(*) from evento_animal").query(Long.class).single();
        assertThat(total).isEqualTo(TipoEventoAnimal.values().length);
    }

    @Test
    void elCheckSigueRechazandoUnTipoDesconocido(@TempDir Path tempDir) {
        JdbcClient jdbc = migrar(tempDir);
        UUID animalId = sembrarAnimal(jdbc);

        assertThatThrownBy(() -> jdbc.sql("insert into evento_animal(id,animal_id,tipo) values(:id,:animal,:tipo)")
                .param("id", UUID.randomUUID().toString())
                .param("animal", animalId.toString())
                .param("tipo", "TIPO_INEXISTENTE")
                .update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("CHECK constraint failed");
    }

    private JdbcClient migrar(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("evento-timeline-test.db") + "?foreign_keys=on");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        return JdbcClient.create(dataSource);
    }

    private UUID sembrarAnimal(JdbcClient jdbc) {
        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values(:id,:c,'Ternera','AMBOS')")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Maternidad',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId)
                .param("p", "00000000-0000-0000-0000-000000000001").update();
        UUID animalId = UUID.randomUUID();
        jdbc.sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                        + "values(:id,:cod,'HEMBRA',:raza,:cat,'CARNE','NACIDO',:pot,:ingreso)")
                .param("id", animalId.toString()).param("cod", "N-" + animalId)
                .param("raza", razaId.toString()).param("cat", categoriaId.toString())
                .param("pot", potreroId.toString()).param("ingreso", LocalDate.now().toString()).update();
        return animalId;
    }
}
