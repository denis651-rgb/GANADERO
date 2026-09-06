package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.sanidad.infrastructure.JdbcJornadaSanitariaRepository;
import bo.com.ganadero.sanidad.infrastructure.JdbcSanidadRepository;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Fase 2 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, secciones 3.2 y 7):
 * historial sanitario declarado al ingreso. Corre contra SQLite real para plan/item y
 * aplicacion_sanitaria (para probar el calculo real de proximaAplicacion y la persistencia
 * de origen_registro), pero mockea AnimalRepository/MotorAlertas — no son el objeto de esta
 * prueba y traerlos reales solo agregaria ruido (AnimalRepository real es package-private
 * en animales.infrastructure; ya se prueba en AnimalServiceBatchIntegrationTest).
 */
class HistorialDeclaradoServiceTest {

    @Test
    void registraVariasActividadesParaVariosAnimalesEnUnaOperacion(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcSanidadRepository planes = new JdbcSanidadRepository(jdbc, new tools.jackson.databind.ObjectMapper());
        JdbcJornadaSanitariaRepository jornadas = new JdbcJornadaSanitariaRepository(jdbc);
        Animal primero = sembrarAnimal(jdbc);
        Animal segundo = sembrarAnimal(jdbc);
        AnimalRepository animales = mock(AnimalRepository.class);
        when(animales.findById(primero.id(), null)).thenReturn(java.util.Optional.of(primero));
        when(animales.findById(segundo.id(), null)).thenReturn(java.util.Optional.of(segundo));
        ObjectProvider<MotorAlertas> alertasProvider = mock(ObjectProvider.class);
        when(alertasProvider.getIfAvailable()).thenReturn(null);
        HistorialDeclaradoService service = new HistorialDeclaradoService(jornadas, planes, animales,
                userContext(), alertasProvider, mock(ApplicationEventPublisher.class));

        LocalDate fecha = LocalDate.now();
        var command = new RegistrarHistorialDeclaradoLoteCommand(List.of(primero.id(), segundo.id()), List.of(
                new RegistrarHistorialDeclaradoLoteCommand.Actividad(TipoActividadSanitaria.VACUNACION,
                        null, fecha, null, null, "Vacuna declarada", "Certificado 123"),
                new RegistrarHistorialDeclaradoLoteCommand.Actividad(TipoActividadSanitaria.DESPARASITACION,
                        null, fecha, null, null, "Ivermectina", "Declarado por el proveedor")
        ));

        List<AplicacionSanitaria> resultado = service.registrarLote(command);

        assertThat(resultado).hasSize(4);
        assertThat(resultado).extracting(AplicacionSanitaria::animalId)
                .containsOnly(primero.id(), segundo.id());
        assertThat(resultado).allMatch(a -> a.origenRegistro() == OrigenRegistroAplicacion.DECLARADA_PROVEEDOR);
    }

    @Test
    void conPlanItemCalculaProximaAplicacionYProgramaLaAlerta(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcSanidadRepository planes = new JdbcSanidadRepository(jdbc, new tools.jackson.databind.ObjectMapper());
        JdbcJornadaSanitariaRepository jornadas = new JdbcJornadaSanitariaRepository(jdbc);
        UUID actor = UUID.randomUUID();

        PlanSanitario plan = planes.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan 2026", null,
                LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0), actor);
        PlanSanitarioItem item = planes.crearItem(new PlanSanitarioItem(UUID.randomUUID(), null, plan.id(),
                TipoActividadSanitaria.VACUNACION, null, "Brucelosis Cepa 19", null, null, null, null, null, null,
                180, 7, null, true, true, 0, OrigenRegulatorioActividad.OBLIGATORIO_SENASAG, "BOVINO"), actor);

        Animal animal = sembrarAnimal(jdbc);
        UUID animalId = animal.id();
        AnimalRepository animales = mock(AnimalRepository.class);
        when(animales.findById(animalId, null)).thenReturn(java.util.Optional.of(animal));

        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ObjectProvider<MotorAlertas> alertasProvider = mock(ObjectProvider.class);
        when(alertasProvider.getIfAvailable()).thenReturn(motorAlertas);

        HistorialDeclaradoService service = new HistorialDeclaradoService(jornadas, planes, animales,
                userContext(), alertasProvider, mock(ApplicationEventPublisher.class));

        LocalDate fechaDeclarada = LocalDate.of(2026, 1, 10);
        AplicacionSanitaria resultado = service.registrar(new RegistrarAplicacionDeclaradaCommand(animalId,
                TipoActividadSanitaria.VACUNACION, item.id(), fechaDeclarada, null, null,
                "Brucelosis Cepa 19", "Según certificado del proveedor Estancia El Roble"));

        assertThat(resultado.jornadaId()).isNull();
        assertThat(resultado.origenRegistro()).isEqualTo(OrigenRegistroAplicacion.DECLARADA_PROVEEDOR);
        assertThat(resultado.proximaAplicacion()).isEqualTo(fechaDeclarada.plusDays(180));
        assertThat(resultado.observaciones()).contains("Historial declarado por el proveedor")
                .contains("Estancia El Roble");
        assertThat(resultado.productoAplicadoTexto()).isEqualTo("Brucelosis Cepa 19");

        AplicacionSanitaria releida = jornadas.aplicacion(resultado.id(), null).orElseThrow();
        assertThat(releida.origenRegistro()).isEqualTo(OrigenRegistroAplicacion.DECLARADA_PROVEEDOR);
        assertThat(releida.proximaAplicacion()).isEqualTo(fechaDeclarada.plusDays(180));

        verify(motorAlertas).programar(any(ProgramarAlertaCommand.class));
    }

    @Test
    void sinPlanItemGuardaElHistorialSinProgramarAlerta(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcSanidadRepository planes = new JdbcSanidadRepository(jdbc, new tools.jackson.databind.ObjectMapper());
        JdbcJornadaSanitariaRepository jornadas = new JdbcJornadaSanitariaRepository(jdbc);

        Animal animal = sembrarAnimal(jdbc);
        UUID animalId = animal.id();
        AnimalRepository animales = mock(AnimalRepository.class);
        when(animales.findById(animalId, null)).thenReturn(java.util.Optional.of(animal));

        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ObjectProvider<MotorAlertas> alertasProvider = mock(ObjectProvider.class);
        when(alertasProvider.getIfAvailable()).thenReturn(motorAlertas);

        HistorialDeclaradoService service = new HistorialDeclaradoService(jornadas, planes, animales,
                userContext(), alertasProvider, mock(ApplicationEventPublisher.class));

        AplicacionSanitaria resultado = service.registrar(new RegistrarAplicacionDeclaradaCommand(animalId,
                TipoActividadSanitaria.VIGILANCIA, null, LocalDate.of(2026, 1, 10), null, null,
                null, "Notificación de ingreso, sin certificado sanitario del vendedor"));

        assertThat(resultado.planItemId()).isNull();
        assertThat(resultado.proximaAplicacion()).isNull();
        assertThat(resultado.origenRegistro()).isEqualTo(OrigenRegistroAplicacion.DECLARADA_PROVEEDOR);
        verifyNoInteractions(motorAlertas);
    }

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * aplicacion_sanitaria.animal_id tiene FK real a animal(id): el animal mockeado en
     * AnimalRepository igual necesita existir en la tabla para que el insert no falle.
     */
    private Animal sembrarAnimal(JdbcClient jdbc) {
        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values(:id,:c,'Vaquillona','AMBOS')")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Potrero de ingreso',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID animalId = UUID.randomUUID();
        jdbc.sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                + "values(:id,:cod,'HEMBRA',:raza,:cat,'CARNE','COMPRADO',:pot,:ingreso)")
                .param("id", animalId.toString()).param("cod", "ANI-" + animalId)
                .param("raza", razaId.toString()).param("cat", categoriaId.toString())
                .param("pot", potreroId.toString()).param("ingreso", LocalDate.now().toString()).update();
        return new Animal(animalId, null, "ANI-" + animalId, null, SexoAnimal.HEMBRA, null, false, razaId,
                categoriaId, null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, PROPIEDAD_ID, potreroId, null,
                EstadoAnimal.ACTIVO, LocalDate.now(), null, null, null, null, null, 0);
    }

    private UserContext userContext() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), null, UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        return new UserContext(() -> currentUser);
    }

    private void migrar(DataSource dataSource) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("historial-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
