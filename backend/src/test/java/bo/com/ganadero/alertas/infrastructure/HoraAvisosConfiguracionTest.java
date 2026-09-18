package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.AlertaConfiguracion;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class HoraAvisosConfiguracionTest {

    @Test
    void sinConfigurarNadaLosAvisosSalenALas0800(@TempDir Path dir) {
        var jdbc = JdbcClient.create(migrar(dir, null));

        assertThat(new JdbcAlertaConfiguracion(jdbc).obtener(null).horaAvisos()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void leeLaHoraConfigurada(@TempDir Path dir) {
        var jdbc = JdbcClient.create(migrar(dir, null));
        jdbc.sql("update configuracion set hora_avisos='09:30'").update();

        assertThat(new JdbcAlertaConfiguracion(jdbc).obtener(null).horaAvisos()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void unaHoraIlegibleNoDejaSinAvisosYCaeEnLaPredeterminada(@TempDir Path dir) {
        var jdbc = JdbcClient.create(migrar(dir, null));
        jdbc.sql("update configuracion set hora_avisos='mañana'").update();

        assertThat(new JdbcAlertaConfiguracion(jdbc).obtener(null).horaAvisos())
                .isEqualTo(AlertaConfiguracion.HORA_AVISOS_PREDETERMINADA);
    }

    @Test
    void laMigracionMueveLosAvisosPendientesDeMedianocheALas0800SinTocarLosDemas(@TempDir Path dir) {
        // Base tal como la deja la versión anterior: avisos de fecha pura programados a medianoche.
        var ds = migrar(dir, "43");
        var jdbc = JdbcClient.create(ds);
        insertar(jdbc, "a1", "PARTO_PROXIMO", "2026-08-15T04:00:00Z", "PROGRAMADA");   // medianoche de La Paz
        insertar(jdbc, "a2", "DESTETE_PROXIMO", "2026-08-20T00:00:00Z", "PROGRAMADA"); // medianoche UTC
        insertar(jdbc, "a3", "VACUNA_PROXIMA", "2026-08-21T04:00:00Z", "PROGRAMADA");
        insertar(jdbc, "a4", "RETIRO_CARNE_VIGENTE", "2026-08-22T04:00:00Z", "PROGRAMADA");
        insertar(jdbc, "a5", "PARTO_PROXIMO", "2026-08-16T04:00:00Z", "PENDIENTE");    // ya emitido: no se toca
        insertar(jdbc, "a6", "PESAJE_ATRASADO", "2026-08-17T04:00:00Z", "PROGRAMADA"); // otro tipo: no se toca
        insertar(jdbc, "a7", "PARTO_PROXIMO", "2026-08-18T10:30:00Z", "PROGRAMADA");   // ya tiene hora real: no se toca

        migrar(dir, null);

        assertThat(fecha(jdbc, "a1")).isEqualTo("2026-08-15T12:00:00Z");
        assertThat(fecha(jdbc, "a2")).isEqualTo("2026-08-20T12:00:00Z");
        assertThat(fecha(jdbc, "a3")).isEqualTo("2026-08-21T12:00:00Z");
        assertThat(fecha(jdbc, "a4")).isEqualTo("2026-08-22T12:00:00Z");
        assertThat(fecha(jdbc, "a5")).isEqualTo("2026-08-16T04:00:00Z");
        assertThat(fecha(jdbc, "a6")).isEqualTo("2026-08-17T04:00:00Z");
        assertThat(fecha(jdbc, "a7")).isEqualTo("2026-08-18T10:30:00Z");
    }

    /** Migra la base de {@code dir} hasta {@code objetivo} (o hasta la última versión si es nulo). */
    private static SQLiteDataSource migrar(Path dir, String objetivo) {
        var ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + dir.resolve("test.db"));
        var config = Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true);
        if (objetivo != null) config.target(objetivo);
        config.load().migrate();
        return ds;
    }

    private static void insertar(JdbcClient jdbc, String id, String tipo, String fecha, String estado) {
        jdbc.sql("""
                        insert into alerta(id, tipo, titulo, mensaje, severidad, fecha_programada, origen_tipo, origen_id,
                                           estado, clave_idempotencia)
                        values(:id, :tipo, 't', 'm', 'INFO', :fecha, 'PRUEBA', :id, :estado, :id)
                        """)
                .param("id", id).param("tipo", tipo).param("fecha", fecha).param("estado", estado).update();
    }

    private static String fecha(JdbcClient jdbc, String id) {
        return jdbc.sql("select fecha_programada from alerta where id=:id").param("id", id)
                .query(String.class).single();
    }
}
