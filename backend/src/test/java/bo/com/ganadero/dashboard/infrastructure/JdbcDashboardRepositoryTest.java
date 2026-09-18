package bo.com.ganadero.dashboard.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas del dashboard contra SQLite real, para que los conteos no dependan de cómo se lea el SQL. */
class JdbcDashboardRepositoryTest {
    private static final String POTRERO = "40000000-0000-0000-0000-000000000001";

    private JdbcClient jdbc;
    private JdbcDashboardRepository repo;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tempDir.resolve("dashboard-test.db") + "?foreign_keys=on");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).load().migrate();
        jdbc = JdbcClient.create(ds);
        jdbc.sql("insert into raza(id,codigo,nombre) values('50000000-0000-0000-0000-000000000001','B','Brahman') on conflict do nothing").update();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values('60000000-0000-0000-0000-000000000001','V','Vaquillona','AMBOS') on conflict do nothing").update();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:p,'P1','Potrero 1','00000000-0000-0000-0000-000000000001',1) on conflict do nothing")
                .param("p", POTRERO).update();
        repo = new JdbcDashboardRepository(jdbc);
    }

    @Test
    void sinPesajeCuentaSoloAnimalesActivosSinPesajeOConElUltimoDeHaceMasDeTreintaDias() {
        String pesadoHoy = animal("A-PESADO-HOY", "ACTIVO");
        String pesadoHaceDiezDias = animal("A-PESADO-10", "ACTIVO");
        String sinPesajes = animal("A-SIN-PESAJES", "ACTIVO");
        String pesajeViejo = animal("A-PESAJE-VIEJO", "ACTIVO");
        String soloPesajeAnulado = animal("A-PESAJE-ANULADO", "ACTIVO");
        pesaje(pesadoHoy, LocalDate.now(), "ACTIVO");
        pesaje(pesadoHaceDiezDias, LocalDate.now().minusDays(10), "ACTIVO");
        pesaje(pesajeViejo, LocalDate.now().minusDays(45), "ACTIVO");
        pesaje(soloPesajeAnulado, LocalDate.now(), "ANULADO");

        // sin pesajes, pesaje de hace 45 días y pesaje solo anulado
        assertThat(repo.countAnimalesSinPesaje(null, true, Set.of())).isEqualTo(3L);
    }

    @Test
    void sinPesajeNoCuentaAnimalesQueYaNoEstanActivosAunqueTenganUnPesajeViejo() {
        // Regresión: sin paréntesis en el SQL, el «or» contaba también a estos animales.
        String vendidoConPesajeViejo = animal("V-VENDIDO", "VENDIDO");
        String muertoConPesajeViejo = animal("V-MUERTO", "MUERTO");
        String transferidoConPesajeViejo = animal("V-TRANSFERIDO", "TRANSFERIDO");
        String activoAlDia = animal("V-ACTIVO", "ACTIVO");
        pesaje(vendidoConPesajeViejo, LocalDate.now().minusDays(90), "ACTIVO");
        pesaje(muertoConPesajeViejo, LocalDate.now().minusDays(200), "ACTIVO");
        pesaje(transferidoConPesajeViejo, LocalDate.now().minusDays(60), "ACTIVO");
        pesaje(activoAlDia, LocalDate.now(), "ACTIVO");

        assertThat(repo.countAnimalesSinPesaje(null, true, Set.of())).isZero();
    }

    @Test
    void potrerosInactivosSoloCuentaLosMarcadosComoInactivos() {
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,'P2','Potrero 2','00000000-0000-0000-0000-000000000001',0)")
                .param("id", UUID.randomUUID().toString()).update();

        assertThat(repo.countPotrerosInactivos(null, true, Set.of())).isEqualTo(1L);
        assertThat(repo.countPotrerosActivos(null, true, Set.of())).isEqualTo(1L);
    }

    private String animal(String codigo, String estado) {
        String id = UUID.randomUUID().toString();
        jdbc.sql("""
                insert into animal(id,codigo,sexo,fecha_nacimiento,raza_principal_id,categoria_actual_id,proposito,origen,
                    potrero_actual_id,fecha_ingreso,estado)
                values(:id,:c,'HEMBRA','2024-01-01','50000000-0000-0000-0000-000000000001',
                    '60000000-0000-0000-0000-000000000001','CARNE','NACIDO',:p,'2024-01-01',:e)
                """).param("id", id).param("c", codigo).param("p", POTRERO).param("e", estado).update();
        return id;
    }

    private void pesaje(String animalId, LocalDate fecha, String estado) {
        jdbc.sql("insert into pesaje(id,animal_id,fecha,peso_kg,estado) values(:id,:a,:f,300,:e)")
                .param("id", UUID.randomUUID().toString()).param("a", animalId).param("f", fecha.toString())
                .param("e", estado).update();
    }
}
