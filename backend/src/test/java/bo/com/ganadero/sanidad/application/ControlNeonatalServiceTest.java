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
import bo.com.ganadero.sanidad.domain.ControlNeonatal;
import bo.com.ganadero.sanidad.domain.EstadoCalostrado;
import bo.com.ganadero.sanidad.domain.MomentoControlNeonatal;
import bo.com.ganadero.sanidad.infrastructure.JdbcClinicaRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Fase 5 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 4.c):
 * control neonatal. Corre contra SQLite real para control_neonatal (para probar la
 * persistencia real incluyendo los checks CHECK de momento/calostrado), pero mockea
 * AnimalRepository/MotorAlertas — no son el objeto de esta prueba (mismo criterio que
 * HistorialDeclaradoServiceTest).
 */
class ControlNeonatalServiceTest {

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void diaCeroConCalostradoCorrectoNoGeneraAlerta(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        AnimalRepository animales = mockAnimalRepository(animal);
        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ClinicaService service = new ClinicaService(repo, animales, userContext(), mock(ObjectProvider.class),
                alertasProvider(motorAlertas), mock(bo.com.ganadero.timeline.application.TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class), mock(ConfiguracionSanitariaService.class));

        ControlNeonatal control = service.crearControl(new CrearControlNeonatalCommand(animal.id(),
                LocalDate.of(2026, 1, 1), MomentoControlNeonatal.DIA_0, EstadoCalostrado.CORRECTO,
                true, "Cicatrizado", false, "Bueno", "Normal", null, null));

        assertThat(control.id()).isNotNull();
        assertThat(control.momento()).isEqualTo(MomentoControlNeonatal.DIA_0);
        verifyNoInteractions(motorAlertas);
    }

    @Test
    void calostradoInsuficienteGeneraRecordatorioSanidadUrgente(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        AnimalRepository animales = mockAnimalRepository(animal);
        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ClinicaService service = new ClinicaService(repo, animales, userContext(), mock(ObjectProvider.class),
                alertasProvider(motorAlertas), mock(bo.com.ganadero.timeline.application.TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class), mock(ConfiguracionSanitariaService.class));

        service.crearControl(new CrearControlNeonatalCommand(animal.id(), LocalDate.of(2026, 1, 1),
                MomentoControlNeonatal.DIA_0, EstadoCalostrado.INSUFICIENTE, true, null, false, null, null, null, null));

        verify(motorAlertas).crearInmediata(argThat((ProgramarAlertaCommand cmd) ->
                cmd.tipo() == TipoAlerta.RECORDATORIO_SANIDAD
                        && "URGENTE".equals(cmd.metadata().get("severidad"))
                        && cmd.metadata().get("mensajePersonalizado").toString().contains("calostrado insuficiente")));
    }

    @Test
    void diarreaGeneraAlerta(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        AnimalRepository animales = mockAnimalRepository(animal);
        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ClinicaService service = new ClinicaService(repo, animales, userContext(), mock(ObjectProvider.class),
                alertasProvider(motorAlertas), mock(bo.com.ganadero.timeline.application.TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class), mock(ConfiguracionSanitariaService.class));

        service.crearControl(new CrearControlNeonatalCommand(animal.id(), LocalDate.of(2026, 1, 4),
                MomentoControlNeonatal.PRIMERA_SEMANA, EstadoCalostrado.CORRECTO, true, null, true, "Decaido", null, null, null));

        verify(motorAlertas).crearInmediata(argThat((ProgramarAlertaCommand cmd) ->
                cmd.tipo() == TipoAlerta.RECORDATORIO_SANIDAD
                        && "URGENTE".equals(cmd.metadata().get("severidad"))
                        && cmd.metadata().get("mensajePersonalizado").toString().contains("diarrea")));
    }

    @Test
    void listaLosControlesDeUnAnimalOrdenadosPorFecha(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        AnimalRepository animales = mockAnimalRepository(animal);
        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ClinicaService service = new ClinicaService(repo, animales, userContext(), mock(ObjectProvider.class),
                alertasProvider(motorAlertas), mock(bo.com.ganadero.timeline.application.TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class), mock(ConfiguracionSanitariaService.class));

        service.crearControl(new CrearControlNeonatalCommand(animal.id(), LocalDate.of(2026, 1, 4),
                MomentoControlNeonatal.PRIMERA_SEMANA, EstadoCalostrado.CORRECTO, true, null, false, null, null, null, null));
        service.crearControl(new CrearControlNeonatalCommand(animal.id(), LocalDate.of(2026, 1, 1),
                MomentoControlNeonatal.DIA_0, EstadoCalostrado.CORRECTO, true, null, false, null, null, null, null));

        List<ControlNeonatal> controles = service.controlesNeonatales(animal.id());

        assertThat(controles).hasSize(2);
        assertThat(controles.get(0).momento()).isEqualTo(MomentoControlNeonatal.DIA_0);
        assertThat(controles.get(1).momento()).isEqualTo(MomentoControlNeonatal.PRIMERA_SEMANA);
    }

    @Test
    void fallaConAnimalNotActiveParaUnAnimalMuerto(@TempDir Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        migrar(dataSource);
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        Animal muerto = new Animal(animal.id(), animal.empresaId(), animal.codigo(), animal.nombre(), animal.sexo(),
                animal.fechaNacimiento(), animal.fechaNacimientoEstimada(), animal.razaPrincipalId(),
                animal.categoriaActualId(), animal.color(), animal.proposito(), animal.origen(),
                animal.propiedadActualId(), animal.potreroActualId(), animal.loteActualId(), EstadoAnimal.MUERTO,
                animal.fechaIngreso(), animal.precioAdquisicion(), animal.pesoNacimientoKg(),
                animal.condicionCorporalActual(), animal.fotoPrincipalPath(), animal.observaciones(), animal.version());
        AnimalRepository animales = mockAnimalRepository(muerto);
        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ClinicaService service = new ClinicaService(repo, animales, userContext(), mock(ObjectProvider.class),
                alertasProvider(motorAlertas), mock(bo.com.ganadero.timeline.application.TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class), mock(ConfiguracionSanitariaService.class));

        assertThatThrownBy(() -> service.crearControl(new CrearControlNeonatalCommand(animal.id(),
                LocalDate.of(2026, 1, 1), MomentoControlNeonatal.DIA_0, EstadoCalostrado.CORRECTO,
                true, null, false, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE);
        verifyNoInteractions(motorAlertas);
    }

    private AnimalRepository mockAnimalRepository(Animal animal) {
        AnimalRepository animales = mock(AnimalRepository.class);
        when(animales.findById(animal.id(), null)).thenReturn(Optional.of(animal));
        return animales;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<MotorAlertas> alertasProvider(MotorAlertas motorAlertas) {
        ObjectProvider<MotorAlertas> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(motorAlertas);
        return provider;
    }

    /** animal.id tiene FK real a animal(id): el animal mockeado igual necesita existir en la tabla. */
    private Animal sembrarAnimal(JdbcClient jdbc) {
        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values(:id,:c,'Ternera','AMBOS')")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Maternidad',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID animalId = UUID.randomUUID();
        jdbc.sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                + "values(:id,'N-000234','HEMBRA',:raza,:cat,'CARNE','NACIDO',:pot,:ingreso)")
                .param("id", animalId.toString()).param("raza", razaId.toString()).param("cat", categoriaId.toString())
                .param("pot", potreroId.toString()).param("ingreso", LocalDate.now().toString()).update();
        return new Animal(animalId, null, "N-000234", null, SexoAnimal.HEMBRA, LocalDate.of(2026, 1, 1), false, razaId,
                categoriaId, null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO, PROPIEDAD_ID, potreroId, null,
                EstadoAnimal.ACTIVO, LocalDate.of(2026, 1, 1), null, null, null, null, null, 0);
    }

    private UserContext userContext() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), null, UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        return new UserContext(() -> currentUser);
    }

    private void migrar(DataSource dataSource) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("control-neonatal-test.db") + "?foreign_keys=on");
        return dataSource;
    }
}
