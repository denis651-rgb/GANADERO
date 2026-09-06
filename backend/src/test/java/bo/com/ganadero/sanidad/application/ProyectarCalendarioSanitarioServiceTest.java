package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.MotorAlertasService;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.alertas.infrastructure.JdbcAlertaRepository;
import bo.com.ganadero.sanidad.domain.EstadoPlanSanitario;
import bo.com.ganadero.sanidad.domain.OrigenRegistroAplicacion;
import bo.com.ganadero.sanidad.domain.OrigenRegulatorioActividad;
import bo.com.ganadero.sanidad.domain.PlanSanitario;
import bo.com.ganadero.sanidad.domain.PlanSanitarioItem;
import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;
import bo.com.ganadero.sanidad.infrastructure.JdbcSanidadRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 3 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, secciones 3 y 5):
 * motor de calendario proyectado. Corre contra SQLite real con MotorAlertasService +
 * JdbcAlertaRepository reales (no mockeados) porque lo central a probar es justamente el
 * comportamiento de persistencia de evolucionar() — que correr procesar() dos veces no
 * duplique filas en `alerta` — algo que un mock no puede demostrar de verdad.
 */
class ProyectarCalendarioSanitarioServiceTest {

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void animalNacidoEnFincaDentroDelMargenGeneraVacunaProxima(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID item = f.crearItemVacunacionBrucelosis();
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        // ventana entra a los 90 dias; nace hoy - 85 dias => faltan 5 dias, dentro de dias_alerta=7
        f.crearAnimalNacidoEnFinca("N-000234", hoy.minusDays(85));

        int procesados = f.service().procesar(hoy);

        assertThat(procesados).isEqualTo(1);
        List<Map<String, Object>> alertas = f.alertasDe(TipoAlerta.VACUNA_PROXIMA);
        assertThat(alertas).hasSize(1);
    }

    @Test
    void animalNacidoEnFincaConVentanaVencidaGeneraVacunaVencida(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearItemVacunacionBrucelosis();
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        // nace hace 200 dias: la ventana (90 dias) quedo muy atras
        f.crearAnimalNacidoEnFinca("N-000235", hoy.minusDays(200));

        int procesados = f.service().procesar(hoy);

        assertThat(procesados).isEqualTo(1);
        assertThat(f.alertasDe(TipoAlerta.VACUNA_VENCIDA)).hasSize(1);
        assertThat(f.alertasDe(TipoAlerta.VACUNA_PROXIMA)).isEmpty();
    }

    @Test
    void animalCompradoSinHistorialDentroDeVentanaGeneraRevisionConDentroDeVentanaTrue(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearItemVacunacionBrucelosis(); // edadMinDias=90, edadMaxDias=240
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        // categoria con edad_min_meses=6 (~180 dias) + fecha_ingreso=hoy => referencia ~= hoy-180
        // ventana entra a referencia+90 = hoy-90 (ya pasada) y sale a referencia+240 = hoy+60 (no llegó)
        f.crearAnimalComprado("LOTE-001", hoy);

        int procesados = f.service().procesar(hoy);

        assertThat(procesados).isEqualTo(1);
        List<Map<String, Object>> alertas = f.alertasDe(TipoAlerta.REVISION_SANITARIA_INGRESO);
        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).get("dentroDeVentana")).isEqualTo(true);
    }

    @Test
    void animalCompradoSinHistorialFueraDeVentanaGeneraRevisionConDentroDeVentanaFalse(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearItemVacunacionBrucelosis(); // edadMaxDias=240
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        // ingreso hace 300 dias: referencia ~= ingreso-180 => hoy la edad estimada ya supera 240 dias
        LocalDate fechaIngreso = hoy.minusDays(300);
        f.crearAnimalComprado("LOTE-002", fechaIngreso);

        f.service().procesar(hoy);

        List<Map<String, Object>> alertas = f.alertasDe(TipoAlerta.REVISION_SANITARIA_INGRESO);
        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).get("dentroDeVentana")).isEqualTo(false);
    }

    @Test
    void animalConAplicacionYaDeclaradaNoGeneraNingunaAlerta(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID item = f.crearItemVacunacionBrucelosis();
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        UUID animalId = f.crearAnimalComprado("LOTE-003", hoy);
        f.declararAplicacion(animalId, item, hoy.minusDays(10));

        int procesados = f.service().procesar(hoy);

        assertThat(procesados).isZero();
        assertThat(f.totalAlertas()).isZero();
    }

    @Test
    void correrProcesarDosVecesNoDuplicaAlertas(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearItemVacunacionBrucelosis();
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        f.crearAnimalNacidoEnFinca("N-000236", hoy.minusDays(200));

        f.service().procesar(hoy);
        f.service().procesar(hoy);
        int totalTrasSegundaCorrida = f.totalAlertas();
        f.service().procesar(hoy.plusDays(1));

        assertThat(totalTrasSegundaCorrida).isEqualTo(1);
        assertThat(f.totalAlertas()).isEqualTo(1);
    }

    private Fixture fixture(Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        MotorAlertas alertas = new MotorAlertasService(new JdbcAlertaRepository(jdbc, JsonMapper.builder().build()));
        ProyectarCalendarioSanitarioService service = new ProyectarCalendarioSanitarioService(jdbc, alertas);
        return new Fixture(jdbc, service);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("proyeccion-test.db") + "?foreign_keys=on");
        return dataSource;
    }

    private record Fixture(JdbcClient jdbc, ProyectarCalendarioSanitarioService service) {

        /** Item de brucelosis: ventana 90-240 dias, alerta con 7 dias de anticipacion. */
        UUID crearItemVacunacionBrucelosis() {
            JdbcSanidadRepository planes = new JdbcSanidadRepository(jdbc, new tools.jackson.databind.ObjectMapper());
            UUID actor = UUID.randomUUID();
            PlanSanitario plan = planes.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan 2026", null,
                    LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0), actor);
            PlanSanitarioItem item = planes.crearItem(new PlanSanitarioItem(UUID.randomUUID(), null, plan.id(),
                    TipoActividadSanitaria.VACUNACION, null, "Brucelosis Cepa 19", null, null, 90, 240, null, null,
                    null, 7, null, true, true, 0, OrigenRegulatorioActividad.OBLIGATORIO_SENASAG, "BOVINO"), actor);
            return item.id();
        }

        UUID crearAnimalNacidoEnFinca(String codigo, LocalDate fechaNacimiento) {
            return crearAnimal(codigo, fechaNacimiento, fechaNacimiento);
        }

        UUID crearAnimalComprado(String codigo, LocalDate fechaIngreso) {
            return crearAnimal(codigo, null, fechaIngreso);
        }

        private UUID crearAnimal(String codigo, LocalDate fechaNacimiento, LocalDate fechaIngreso) {
            UUID razaId = UUID.randomUUID();
            jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                    .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
            // categoria con edad_min_meses=6 (~180 dias), usada para estimar la fecha de referencia de compras.
            UUID categoriaId = UUID.randomUUID();
            jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable,edad_min_meses) values(:id,:c,'Vaquillona','AMBOS',6)")
                    .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
            UUID potreroId = UUID.randomUUID();
            jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Potrero de ingreso',:p,1)")
                    .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
            UUID animalId = UUID.randomUUID();
            jdbc.sql("insert into animal(id,codigo,sexo,fecha_nacimiento,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                    + "values(:id,:cod,'HEMBRA',:fn,:raza,:cat,'CARNE',:origen,:pot,:ingreso)")
                    .param("id", animalId.toString()).param("cod", codigo)
                    .param("fn", fechaNacimiento == null ? null : fechaNacimiento.toString())
                    .param("raza", razaId.toString()).param("cat", categoriaId.toString())
                    .param("origen", fechaNacimiento == null ? "COMPRADO" : "NACIDO")
                    .param("pot", potreroId.toString()).param("ingreso", fechaIngreso.toString()).update();
            return animalId;
        }

        void declararAplicacion(UUID animalId, UUID planItemId, LocalDate fechaAplicacion) {
            jdbc.sql("insert into aplicacion_sanitaria(id,jornada_id,plan_item_id,animal_id,fecha_aplicacion,idempotency_key,estado,origen_registro) "
                    + "values(:id,null,:item,:animal,:fecha,:key,'APLICADO',:origen)")
                    .param("id", UUID.randomUUID().toString()).param("item", planItemId.toString())
                    .param("animal", animalId.toString()).param("fecha", fechaAplicacion.toString())
                    .param("key", UUID.randomUUID().toString()).param("origen", OrigenRegistroAplicacion.DECLARADA_PROVEEDOR.name())
                    .update();
        }

        List<Map<String, Object>> alertasDe(TipoAlerta tipo) {
            return jdbc.sql("select metadata from alerta where tipo = :t")
                    .param("t", tipo.name())
                    .query((rs, rowNum) -> JsonMapper.builder().build()
                            .readValue(rs.getString("metadata"), new TypeReference<Map<String, Object>>() { }))
                    .list();
        }

        int totalAlertas() {
            return jdbc.sql("select count(*) as n from alerta").query(Integer.class).single();
        }
    }
}
