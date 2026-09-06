package bo.com.ganadero.alertas.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class ConfiguracionReproduccionTest {
 @Test void leeConfiguracionTrasMigracionMultiPropiedad(@TempDir Path dir) {
  var ds = new SQLiteDataSource();
  ds.setUrl("jdbc:sqlite:" + dir.resolve("test.db"));
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).load().migrate();
  var jdbc = JdbcClient.create(ds);
  jdbc.sql("update configuracion set dias_diagnostico_post_servicio=35, dias_gestacion_estimada=280").update();
  var config = new JdbcAlertaConfiguracion(jdbc).obtener(null);
  assertThat(config.diasDiagnosticoPostServicio()).isEqualTo(35);
  assertThat(config.diasGestacionEstimada()).isEqualTo(280);
 }
}
