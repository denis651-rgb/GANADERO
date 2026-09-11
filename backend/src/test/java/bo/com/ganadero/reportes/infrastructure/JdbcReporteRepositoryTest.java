package bo.com.ganadero.reportes.infrastructure;

import bo.com.ganadero.reportes.domain.ReporteAnimalMuerto;
import bo.com.ganadero.reportes.domain.ReporteAnimalNacido;
import bo.com.ganadero.reportes.domain.ReporteVenta;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corre las migraciones Flyway reales contra un SQLite descartable (mismo patrón que
 * JdbcSanidadRepositoryTest) para confirmar las 3 consultas del reporte de movimientos del hato,
 * en particular que la muerte de un animal (que solo vive como JSON en evento_animal.metadata,
 * no en sus columnas planas) se lee correctamente con json_extract.
 */
class JdbcReporteRepositoryTest {
    private static final String PROPIEDAD = "00000000-0000-0000-0000-000000000001";
    private static final String RAZA = "50000000-0000-0000-0000-000000000001";
    private static final String CATEGORIA_TERNERO = "60000000-0000-0000-0000-000000000001";
    private static final String CATEGORIA_VACA = "60000000-0000-0000-0000-000000000005";

    @Test
    void listaAnimalesNacidosEnElRangoConSuMadreYPadre(@TempDir Path tempDir) {
        JdbcClient jdbc = migrar(tempDir);
        String potrero = crearPotrero(jdbc);
        String madre = crearAnimal(jdbc, "MAD-0001", "Luna", "HEMBRA", CATEGORIA_VACA, potrero, "NACIDO", "2020-01-01");
        crearAnimal(jdbc, "PAD-0001", "Toro Bravo", "MACHO", CATEGORIA_VACA, potrero, "NACIDO", "2019-01-01");
        String cria = crearAnimal(jdbc, "CRI-0001", "Estrella", "HEMBRA", CATEGORIA_TERNERO, potrero, "NACIDO", "2026-02-10");
        crearAnimal(jdbc, "FUERA-0001", "Fuera de rango", "HEMBRA", CATEGORIA_TERNERO, potrero, "NACIDO", "2025-01-01");
        jdbc.sql("insert into parentesco(id,animal_id,tipo_parentesco,animal_padre_id) values(:id,:animal,'MADRE',:madre)")
                .param("id", UUID.randomUUID().toString()).param("animal", cria).param("madre", madre).update();
        jdbc.sql("insert into parentesco(id,animal_id,tipo_parentesco,nombre_externo) values(:id,:animal,'PADRE',:padre)")
                .param("id", UUID.randomUUID().toString()).param("animal", cria).param("padre", "Toro externo").update();

        JdbcReporteRepository repo = new JdbcReporteRepository(jdbc);
        List<ReporteAnimalNacido> nacidos = repo.nacidos(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"));

        assertThat(nacidos).extracting(ReporteAnimalNacido::codigo).containsExactly("CRI-0001");
        ReporteAnimalNacido reporte = nacidos.get(0);
        assertThat(reporte.raza()).isEqualTo("Brahman");
        assertThat(reporte.categoria()).isEqualTo("Ternero");
        assertThat(reporte.madre()).isEqualTo("Luna");
        assertThat(reporte.padre()).isEqualTo("Toro externo");
    }

    @Test
    void listaAnimalesMuertosEnElRangoLeyendoElMotivoDesdeElMetadataJson(@TempDir Path tempDir) {
        JdbcClient jdbc = migrar(tempDir);
        String potrero = crearPotrero(jdbc);
        String animal = crearAnimal(jdbc, "VAC-0001", "Paloma", "HEMBRA", CATEGORIA_VACA, potrero, "COMPRADO", "2022-01-01");
        registrarMuerte(jdbc, animal, "2026-03-15T10:00:00Z", "Neumonía");
        String fueraDeRango = crearAnimal(jdbc, "VAC-0002", "Otra", "HEMBRA", CATEGORIA_VACA, potrero, "COMPRADO", "2022-01-01");
        registrarMuerte(jdbc, fueraDeRango, "2025-01-01T10:00:00Z", "Vieja");

        JdbcReporteRepository repo = new JdbcReporteRepository(jdbc);
        List<ReporteAnimalMuerto> muertos = repo.muertos(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"));

        assertThat(muertos).hasSize(1);
        ReporteAnimalMuerto reporte = muertos.get(0);
        assertThat(reporte.codigo()).isEqualTo("VAC-0001");
        assertThat(reporte.motivo()).isEqualTo("Neumonía");
        assertThat(reporte.fechaMuerte()).isEqualTo(LocalDate.parse("2026-03-15"));
    }

    @Test
    void listaVentasEnElRangoConDatosDelAnimal(@TempDir Path tempDir) {
        JdbcClient jdbc = migrar(tempDir);
        String potrero = crearPotrero(jdbc);
        String animal = crearAnimal(jdbc, "VAC-0003", "Rocío", "HEMBRA", CATEGORIA_VACA, potrero, "COMPRADO", "2022-01-01");
        crearVenta(jdbc, animal, "2026-05-01", "Frigorífico Norte", new java.math.BigDecimal("5000"));
        String otroAnimal = crearAnimal(jdbc, "VAC-0004", "Nube", "HEMBRA", CATEGORIA_VACA, potrero, "COMPRADO", "2022-01-01");
        crearVenta(jdbc, otroAnimal, "2025-01-01", "Comprador viejo", new java.math.BigDecimal("100"));

        JdbcReporteRepository repo = new JdbcReporteRepository(jdbc);
        List<ReporteVenta> ventas = repo.ventas(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"));

        assertThat(ventas).hasSize(1);
        assertThat(ventas.get(0).codigo()).isEqualTo("VAC-0003");
        assertThat(ventas.get(0).comprador()).isEqualTo("Frigorífico Norte");
        assertThat(ventas.get(0).raza()).isEqualTo("Brahman");
    }

    private String crearPotrero(JdbcClient jdbc) {
        String id = UUID.randomUUID().toString();
        jdbc.sql("insert into potrero(id,codigo,nombre) values(:id,:codigo,:nombre)")
                .param("id", id).param("codigo", "POT-" + id.substring(0, 8)).param("nombre", "Potrero Norte").update();
        return id;
    }

    private String crearAnimal(JdbcClient jdbc, String codigo, String nombre, String sexo, String categoria,
                               String potrero, String origen, String fechaNacimiento) {
        String id = UUID.randomUUID().toString();
        jdbc.sql("""
                insert into animal(id,codigo,nombre,sexo,fecha_nacimiento,raza_principal_id,categoria_actual_id,
                    proposito,origen,propiedad_actual_id,potrero_actual_id,estado,fecha_ingreso)
                values(:id,:codigo,:nombre,:sexo,:nacimiento,:raza,:categoria,'CARNE',:origen,:propiedad,:potrero,'ACTIVO',:ingreso)
                """)
                .param("id", id).param("codigo", codigo).param("nombre", nombre).param("sexo", sexo)
                .param("nacimiento", fechaNacimiento).param("raza", RAZA).param("categoria", categoria)
                .param("origen", origen).param("propiedad", PROPIEDAD).param("potrero", potrero)
                .param("ingreso", fechaNacimiento)
                .update();
        return id;
    }

    /** Imita lo que graba AnimalService.changeState + JdbcTimelineRepository.insert: motivo/estadoNuevo solo en el JSON. */
    private void registrarMuerte(JdbcClient jdbc, String animalId, String fechaEvento, String motivo) {
        String metadata = "{\"estadoAnterior\":\"ACTIVO\",\"estadoNuevo\":\"MUERTO\",\"motivo\":\"" + motivo + "\"}";
        jdbc.sql("insert into evento_animal(id,animal_id,tipo,fecha_evento,metadata) values(:id,:animal,'ESTADO_CAMBIADO',:fecha,:metadata)")
                .param("id", UUID.randomUUID().toString()).param("animal", animalId).param("fecha", fechaEvento)
                .param("metadata", metadata).update();
        jdbc.sql("update animal set estado='MUERTO' where id=:id").param("id", animalId).update();
    }

    private void crearVenta(JdbcClient jdbc, String animalId, String fechaVenta, String comprador, java.math.BigDecimal precio) {
        jdbc.sql("""
                insert into venta(id,animal_id,fecha_venta,comprador,precio,moneda,modalidad)
                values(:id,:animal,:fecha,:comprador,:precio,'BOB','EN_PIE')
                """)
                .param("id", UUID.randomUUID().toString()).param("animal", animalId).param("fecha", fechaVenta)
                .param("comprador", comprador).param("precio", precio).update();
    }

    private JdbcClient migrar(Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        return JdbcClient.create(dataSource);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("reportes-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
