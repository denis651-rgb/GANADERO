package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.sanidad.domain.EstadoPlanSanitario;
import bo.com.ganadero.sanidad.domain.OrigenRegulatorioActividad;
import bo.com.ganadero.sanidad.domain.PlanSanitario;
import bo.com.ganadero.sanidad.domain.PlanSanitarioItem;
import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre las migraciones Flyway reales (incluyendo V3__clasificacion_regulatoria.sql) contra
 * un archivo SQLite descartable, para confirmar que la clasificacion regulatoria (Fase 1 del
 * plan sanitario, docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md) persiste de verdad y que aftosa
 * ya es representable como VIGILANCIA_EPIDEMIOLOGICA con origen CAMPANA_RIESGO, sin frecuencia
 * fija de vacunacion.
 */
class JdbcSanidadRepositoryTest {

    @Test
    void persisteUnItemDeVigilanciaEpidemiologicaConSuClasificacionRegulatoria(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcSanidadRepository repo = new JdbcSanidadRepository(JdbcClient.create(dataSource));
        UUID actor = UUID.randomUUID();

        PlanSanitario plan = repo.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan sanitario 2026", null,
                LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0), actor);

        PlanSanitarioItem aftosa = repo.crearItem(new PlanSanitarioItem(UUID.randomUUID(), null, plan.id(),
                TipoActividadSanitaria.VIGILANCIA_EPIDEMIOLOGICA, null, "Notificacion de sospecha de aftosa",
                null, null, null, null, null, null, null, 0, null, false, true, 0,
                OrigenRegulatorioActividad.CAMPANA_RIESGO, "BOVINO"), actor);

        assertThat(aftosa.tipoActividad()).isEqualTo(TipoActividadSanitaria.VIGILANCIA_EPIDEMIOLOGICA);
        assertThat(aftosa.origenRegulatorio()).isEqualTo(OrigenRegulatorioActividad.CAMPANA_RIESGO);
        assertThat(aftosa.especieAplicable()).isEqualTo("BOVINO");
        assertThat(aftosa.frecuenciaDias()).isNull();

        PlanSanitarioItem recargado = repo.items(plan.id(), null, true).stream()
                .filter(item -> item.id().equals(aftosa.id())).findFirst().orElseThrow();
        assertThat(recargado.origenRegulatorio()).isEqualTo(OrigenRegulatorioActividad.CAMPANA_RIESGO);
        assertThat(recargado.especieAplicable()).isEqualTo("BOVINO");
    }

    @Test
    void unItemSinClasificacionExplicitaCaeEnConfigurableEstablecimiento(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcSanidadRepository repo = new JdbcSanidadRepository(JdbcClient.create(dataSource));
        UUID actor = UUID.randomUUID();

        PlanSanitario plan = repo.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan sanitario 2026", null,
                LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0), actor);

        PlanSanitarioItem desparasitacion = repo.crearItem(new PlanSanitarioItem(UUID.randomUUID(), null, plan.id(),
                TipoActividadSanitaria.DESPARASITACION, null, "Ivermectina", null, null, null, null, null, null,
                180, 7, null, false, true, 0,
                OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO"), actor);

        assertThat(desparasitacion.origenRegulatorio()).isEqualTo(OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("sanidad-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
