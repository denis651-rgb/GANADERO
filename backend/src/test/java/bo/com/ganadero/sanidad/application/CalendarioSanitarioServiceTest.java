package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.sanidad.infrastructure.JdbcSanidadRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Corre contra SQLite real: lo central a probar es que dos corridas del generador no dupliquen
 * eventos (el índice único de evento_calendario_sanitario hace el trabajo, pero sólo una prueba
 * de verdad contra la base lo demuestra) y que cada modalidad calcule la fecha prevista según su
 * propia regla (sección 20).
 */
class CalendarioSanitarioServiceTest {
    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void generaUnEventoPorEdadCuandoElAnimalEntraEnLaVentana(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        LocalDate hoy = LocalDate.of(2026, 1, 10);
        f.crearAnimal("N-001", hoy.minusDays(87), false);

        int generados = f.service.procesar();

        assertThat(generados).isEqualTo(1);
        assertThat(f.contarEventos()).isEqualTo(1);
    }

    @Test
    void noGeneraEventoPorEdadTodaviaFueraDeLaVentana(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        f.crearAnimal("N-002", LocalDate.now().minusDays(10), false);

        int generados = f.service.procesar();

        assertThat(generados).isZero();
    }

    @Test
    void noDuplicaElEventoPorEdadAlCorrerElGeneradorDosVeces(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        f.crearAnimal("N-003", LocalDate.now().minusDays(89), false);

        f.service.procesar();
        f.service.procesar();

        assertThat(f.contarEventos()).isEqualTo(1);
    }

    @Test
    void excluyeAnimalesConEdadDesconocidaDeLaModalidadPorEdad(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        f.crearAnimalSinFechaNacimiento("COMPRADO-01");

        int generados = f.service.procesar();

        assertThat(generados).isZero();
        assertThat(f.contarEventos()).isZero();
    }

    @Test
    void generaUnEventoPeriodicaDesdeLaUltimaAplicacionConfirmada(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID itemId = f.crearActividadPeriodica(90, 5, 0);
        UUID animalId = f.crearAnimal("N-004", LocalDate.now().minusYears(2), false);
        f.confirmarAplicacion(animalId, itemId, LocalDate.now().minusDays(88));

        int generados = f.service.procesar();

        assertThat(generados).isEqualTo(1);
        assertThat(f.contarEventos()).isEqualTo(1);
    }

    @Test
    void sinAntecedentePeriodicaUsaLaVigenciaDeLaActividadComoSemilla(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        // vigenteDesde queda en Instant.now() al crear el item; con frecuencia 0 el ciclo cae hoy mismo.
        f.crearActividadPeriodica(0, 5, 0);
        f.crearAnimal("N-005", LocalDate.now().minusYears(1), false);

        int generados = f.service.procesar();

        assertThat(generados).isEqualTo(1);
    }

    private Fixture fixture(Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcSanidadRepository planes = new JdbcSanidadRepository(jdbc, new tools.jackson.databind.ObjectMapper());
        @SuppressWarnings("unchecked")
        ObjectProvider<bo.com.ganadero.alertas.application.MotorAlertas> sinAlertas = mock(ObjectProvider.class);
        CalendarioSanitarioService service = new CalendarioSanitarioService(planes, jdbc,
                new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(jdbc), sinAlertas);
        return new Fixture(jdbc, planes, service);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("calendario-test.db") + "?foreign_keys=on");
        return dataSource;
    }

    private record Fixture(JdbcClient jdbc, JdbcSanidadRepository planes, CalendarioSanitarioService service) {

        UUID crearActividadPorEdad(int edadObjetivoDias, int ventanaAnticipadaDias, int ventanaPosteriorDias) {
            UUID actor = UUID.randomUUID();
            PlanSanitario plan = planes.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan 2026", null,
                    LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0, null), actor);
            PlanSanitarioItem item = construir(plan.id(), ModalidadActividad.POR_EDAD,
                    new ModalidadConfig.PorEdadConfig(edadObjetivoDias, UnidadEdadActividad.DIAS, ventanaAnticipadaDias,
                            ventanaPosteriorDias, PoliticaEdadEstimada.PERMITIR, PoliticaEdadDesconocida.EXCLUIR, true));
            return planes.crearItem(item, actor).id();
        }

        UUID crearActividadPeriodica(int frecuenciaDias, int toleranciaAnticipada, int toleranciaPosterior) {
            UUID actor = UUID.randomUUID();
            PlanSanitario plan = planes.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan 2026", null,
                    LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0, null), actor);
            PlanSanitarioItem item = construir(plan.id(), ModalidadActividad.PERIODICA,
                    new ModalidadConfig.PeriodicaConfig(frecuenciaDias, UnidadFrecuencia.DIAS,
                            ReferenciaCalculoPeriodica.ULTIMA_APLICACION, toleranciaAnticipada, toleranciaPosterior));
            return planes.crearItem(item, actor).id();
        }

        private PlanSanitarioItem construir(UUID planId, ModalidadActividad modalidad, ModalidadConfig config) {
            UUID id = UUID.randomUUID();
            return new PlanSanitarioItem(id, null, planId, TipoActividadSanitaria.DESPARASITACION, null,
                    "Ivermectina 1%", null, null, null, null, null, null, null, 0, null, false, true, 0,
                    OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", false, id, 1, null,
                    Instant.now(), null, null, null, "Desparasitación", null, "Ivermectina", null, null, null, null,
                    null, TipoCalculoDosis.NO_APLICA, null, null, null, null, null, null, null, List.of(),
                    UnidadEdadActividad.DIAS, modalidad, config, false);
        }

        UUID crearAnimal(String codigo, LocalDate fechaNacimiento, boolean estimada) {
            sembrarCatalogos();
            UUID animalId = UUID.randomUUID();
            jdbc.sql("""
                    insert into animal(id,codigo,sexo,fecha_nacimiento,fecha_nacimiento_estimada,raza_principal_id,
                        categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso)
                    values(:id,:cod,'HEMBRA',:fn,:est,'50000000-0000-0000-0000-000000000001',
                        '60000000-0000-0000-0000-000000000001','CARNE','NACIDO','40000000-0000-0000-0000-000000000001',:ingreso)
                    """).param("id", animalId.toString()).param("cod", codigo).param("fn", fechaNacimiento.toString())
                    .param("est", estimada).param("ingreso", fechaNacimiento.toString()).update();
            return animalId;
        }

        UUID crearAnimalSinFechaNacimiento(String codigo) {
            sembrarCatalogos();
            UUID animalId = UUID.randomUUID();
            jdbc.sql("""
                    insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,
                        potrero_actual_id,fecha_ingreso)
                    values(:id,:cod,'HEMBRA','50000000-0000-0000-0000-000000000001',
                        '60000000-0000-0000-0000-000000000001','CARNE','COMPRADO','40000000-0000-0000-0000-000000000001',:ingreso)
                    """).param("id", animalId.toString()).param("cod", codigo).param("ingreso", LocalDate.now().toString()).update();
            return animalId;
        }

        private void sembrarCatalogos() {
            jdbc.sql("insert into raza(id,codigo,nombre) values('50000000-0000-0000-0000-000000000001','BRAHMAN','Brahman') on conflict do nothing").update();
            jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values('60000000-0000-0000-0000-000000000001','VAQUILLONA','Vaquillona','AMBOS') on conflict do nothing").update();
            jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values('40000000-0000-0000-0000-000000000001','POT-01','Potrero',:p,1) on conflict do nothing")
                    .param("p", PROPIEDAD_ID.toString()).update();
        }

        void confirmarAplicacion(UUID animalId, UUID itemId, LocalDate fechaAplicacion) {
            jdbc.sql("""
                    insert into aplicacion_sanitaria(id,plan_item_id,animal_id,fecha_aplicacion,idempotency_key,estado,origen_registro)
                    values(:id,:item,:animal,:fecha,:key,'APLICADO','APLICADA_FINCA')
                    """).param("id", UUID.randomUUID().toString()).param("item", itemId.toString())
                    .param("animal", animalId.toString()).param("fecha", fechaAplicacion.toString())
                    .param("key", UUID.randomUUID().toString()).update();
        }

        int contarEventos() {
            return jdbc.sql("select count(*) from evento_calendario_sanitario").query(Integer.class).single();
        }
    }
}
