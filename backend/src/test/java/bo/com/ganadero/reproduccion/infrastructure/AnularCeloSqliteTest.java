package bo.com.ganadero.reproduccion.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Path;
import java.util.UUID;
import bo.com.ganadero.reproduccion.domain.EstadoRegistroReproduccion;
import bo.com.ganadero.shared.error.BusinessException;
import static org.assertj.core.api.Assertions.*;

class AnularCeloSqliteTest {
 @Test void anulaConFechaMotivoYVersionSinBorrarHistorial(@TempDir Path dir) {
  var ds = new SQLiteDataSource();
  ds.setUrl("jdbc:sqlite:" + dir.resolve("test.db"));
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).load().migrate();
  var jdbc = JdbcClient.create(ds);
  UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
  jdbc.sql("insert into celo(id,animal_id,fecha_deteccion,tipo_deteccion,observaciones) values(:id,:animal,'2026-09-04T13:29:00Z','VISUAL','Observación original')")
      .param("id", id.toString()).param("animal", UUID.randomUUID().toString()).update();
  var repo = new JdbcReproduccionRepository(jdbc);
  assertThatThrownBy(() -> repo.annulCelo(id, null, "Duplicado", 9, actor)).isInstanceOf(BusinessException.class);
  var result = repo.annulCelo(id, null, "Duplicado", 0, actor);
  assertThat(result.estado()).isEqualTo(EstadoRegistroReproduccion.ANULADO);
  assertThat(result.version()).isEqualTo(1);
  assertThat(result.observaciones()).isEqualTo("Observación original");
  assertThat(jdbc.sql("select motivo_anulacion from celo where id=:id").param("id", id.toString()).query(String.class).single()).isEqualTo("Duplicado");
  String fecha = jdbc.sql("select anulado_at from celo where id=:id").param("id", id.toString()).query(String.class).single();
  assertThat(java.time.Instant.parse(fecha)).isNotNull();
  assertThatThrownBy(() -> repo.annulCelo(id, null, "Otra vez", 1, actor)).isInstanceOf(BusinessException.class);
 }
}
