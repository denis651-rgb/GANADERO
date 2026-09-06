package bo.com.ganadero.reproduccion.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import bo.com.ganadero.reproduccion.domain.EstadoServicio;
import static org.assertj.core.api.Assertions.assertThat;

class EstadoServicioSqliteTest {
 @Test void actualizaResultadosDeDiagnosticoSinNowYRespetaAnulados(@TempDir Path dir) {
  var ds = new SQLiteDataSource();
  ds.setUrl("jdbc:sqlite:" + dir.resolve("test.db"));
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).load().migrate();
  var jdbc = JdbcClient.create(ds);
  UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
  jdbc.sql("insert into servicio(id,hembra_id,fecha_servicio,tipo_servicio,fecha_diagnostico_recomendada) values(:id,:hembra,'2026-09-04T13:55:00Z','INSEMINACION_ARTIFICIAL','2026-10-04T13:55:00Z')")
      .param("id", id.toString()).param("hembra", UUID.randomUUID().toString()).update();
  var repo = new JdbcReproduccionRepository(jdbc);
  long version = 0;
  for (var estado : new EstadoServicio[] { EstadoServicio.PENDIENTE_DIAGNOSTICO, EstadoServicio.GESTACION_CONFIRMADA, EstadoServicio.NO_PRENADA, EstadoServicio.FINALIZADO }) {
   repo.updateServicioEstado(id, null, estado, actor);
   var row = jdbc.sql("select estado,version,updated_by,updated_at from servicio where id=:id").param("id", id.toString()).query().singleRow();
   assertThat(row.get("estado")).isEqualTo(estado.name());
   assertThat(((Number) row.get("version")).longValue()).isEqualTo(++version);
   assertThat(row.get("updated_by")).isEqualTo(actor.toString());
   assertThat(Instant.parse((String) row.get("updated_at"))).isNotNull();
  }
  jdbc.sql("update servicio set estado='ANULADO' where id=:id").param("id", id.toString()).update();
  repo.updateServicioEstado(id, null, EstadoServicio.GESTACION_CONFIRMADA, actor);
  assertThat(jdbc.sql("select estado from servicio where id=:id").param("id", id.toString()).query(String.class).single()).isEqualTo("ANULADO");
 }
}
