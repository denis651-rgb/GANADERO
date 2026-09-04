package bo.com.ganadero.reproduccion.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import bo.com.ganadero.reproduccion.domain.*;
import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticoSqliteTest {
 @Test void guardaYRecuperaDiagnosticosConDiasNulosOCargados(@TempDir Path dir) {
  var ds = new SQLiteDataSource();
  ds.setUrl("jdbc:sqlite:" + dir.resolve("test.db"));
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
  var repo = new JdbcReproduccionRepository(JdbcClient.create(ds));
  UUID actor = UUID.randomUUID(), animal = UUID.randomUUID();
  for (Integer dias : new Integer[] { null, 0, 60 }) {
   UUID id = UUID.randomUUID();
   var input = new DiagnosticoGestacion(id, null, animal, null, Instant.parse("2026-09-04T13:58:00Z"),
       dias == null ? ResultadoGestacion.DUDOSO : ResultadoGestacion.POSITIVO, null, dias, null, null,
       "Prueba de lectura", null, null, null, UUID.randomUUID(), null, EstadoRegistroReproduccion.ACTIVO,
       null, null, null, null, 0);
   var guardado = repo.createDiagnostico(input, actor);
   assertThat(guardado.id()).isEqualTo(id);
   assertThat(guardado.diasGestacionEstimados()).isEqualTo(dias);
   assertThat(guardado.resultado()).isEqualTo(input.resultado());
   assertThat(repo.findDiagnosticoById(id, null).orElseThrow().diasGestacionEstimados()).isEqualTo(dias);
  }
 }
}
