package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.ControlEctoparasitario;
import bo.com.ganadero.sanidad.domain.NivelCargaParasitaria;
import bo.com.ganadero.sanidad.domain.TipoEctoparasito;
import bo.com.ganadero.sanidad.domain.EventoCalendarioSanitarioRepository;
import bo.com.ganadero.sanidad.domain.SanidadRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fase 5 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 4.d):
 * control ectoparasitario. Puede registrarse contra un animal o un lote_ganadero
 * completo — nunca ambos, nunca ninguno (mismo criterio de prueba que
 * ControlNeonatalServiceTest: SQLite real para la tabla, mocks para AnimalRepository).
 */
class ControlEctoparasitarioServiceTest {

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void conAnimalIdValidoSeGuardaCorrectamente(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        ClinicaService service = service(repo, mockAnimalRepository(animal));

        ControlEctoparasitario control = service.crearControlEcto(new CrearControlEctoparasitarioCommand(
                animal.id(), null, TipoEctoparasito.GARRAPATA, NivelCargaParasitaria.ALTO, false,
                null, "Cipermetrina", LocalDate.of(2026, 1, 1), null));

        assertThat(control.id()).isNotNull();
        assertThat(control.animalId()).isEqualTo(animal.id());
        assertThat(control.loteGanaderoId()).isNull();
    }

    @Test
    void conLoteGanaderoIdValidoSeGuardaCorrectamente(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        UUID loteId = sembrarLote(jdbc, "Recría 2026");
        ClinicaService service = service(repo, mock(AnimalRepository.class));

        ControlEctoparasitario control = service.crearControlEcto(new CrearControlEctoparasitarioCommand(
                null, loteId, TipoEctoparasito.GARRAPATA, NivelCargaParasitaria.ALTO, true,
                "Bayticol", "Cipermetrina", LocalDate.of(2026, 1, 1), null));

        assertThat(control.id()).isNotNull();
        assertThat(control.animalId()).isNull();
        assertThat(control.loteGanaderoId()).isEqualTo(loteId);
    }

    @Test
    void conAmbosDestinosFallaConDestinoInvalido(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        UUID loteId = sembrarLote(jdbc, "Recría 2026");
        ClinicaService service = service(repo, mockAnimalRepository(animal));

        assertThatThrownBy(() -> service.crearControlEcto(new CrearControlEctoparasitarioCommand(
                animal.id(), loteId, TipoEctoparasito.GARRAPATA, NivelCargaParasitaria.ALTO, false,
                null, null, LocalDate.of(2026, 1, 1), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.SANIDAD_CONTROL_ECTO_DESTINO_INVALIDO);
    }

    @Test
    void sinNingunDestinoFallaConDestinoInvalido(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        ClinicaService service = service(repo, mock(AnimalRepository.class));

        assertThatThrownBy(() -> service.crearControlEcto(new CrearControlEctoparasitarioCommand(
                null, null, TipoEctoparasito.GARRAPATA, NivelCargaParasitaria.ALTO, false,
                null, null, LocalDate.of(2026, 1, 1), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.SANIDAD_CONTROL_ECTO_DESTINO_INVALIDO);
    }

    @Test
    void principiosActivosRecientesDevuelveLosUltimosTresEnOrden(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal animal = sembrarAnimal(jdbc);
        ClinicaService service = service(repo, mockAnimalRepository(animal));

        String[] principios = {"Cipermetrina", "Amitraz", "Fipronil", "Deltametrina", "Ivermectina"};
        for (int i = 0; i < principios.length; i++) {
            service.crearControlEcto(new CrearControlEctoparasitarioCommand(animal.id(), null,
                    TipoEctoparasito.GARRAPATA, NivelCargaParasitaria.MEDIO, true, null, principios[i],
                    LocalDate.of(2026, 1, i + 1), null));
        }

        List<String> recientes = service.principiosActivosRecientes(animal.id(), null);

        assertThat(recientes).containsExactly("Ivermectina", "Deltametrina", "Fipronil");
    }

    private ClinicaService service(JdbcClinicaRepository repo, AnimalRepository animales) {
        MotorAlertas motorAlertas = mock(MotorAlertas.class);
        ObjectProvider<MotorAlertas> alertasProvider = mock(ObjectProvider.class);
        when(alertasProvider.getIfAvailable()).thenReturn(motorAlertas);
        return new ClinicaService(repo, animales, userContext(), mock(SanidadRepository.class), mock(EventoCalendarioSanitarioRepository.class), alertasProvider,
                mock(bo.com.ganadero.timeline.application.TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class), mock(ConfiguracionSanitariaService.class));
    }

    private AnimalRepository mockAnimalRepository(Animal animal) {
        AnimalRepository animales = mock(AnimalRepository.class);
        when(animales.findById(animal.id(), null)).thenReturn(Optional.of(animal));
        return animales;
    }

    private UUID sembrarLote(JdbcClient jdbc, String nombre) {
        UUID loteId = UUID.randomUUID();
        jdbc.sql("insert into lote_ganadero(id,codigo,nombre) values(:id,:c,:n)")
                .param("id", loteId.toString()).param("c", "LOTE-" + loteId).param("n", nombre).update();
        return loteId;
    }

    /** animal_id tiene FK real a animal(id): el animal mockeado igual necesita existir en la tabla. */
    private Animal sembrarAnimal(JdbcClient jdbc) {
        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values(:id,:c,'Recría','AMBOS')")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Recría',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID animalId = UUID.randomUUID();
        jdbc.sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                + "values(:id,'N-000234','HEMBRA',:raza,:cat,'CARNE','NACIDO',:pot,:ingreso)")
                .param("id", animalId.toString()).param("raza", razaId.toString()).param("cat", categoriaId.toString())
                .param("pot", potreroId.toString()).param("ingreso", LocalDate.now().toString()).update();
        return new Animal(animalId, null, "N-000234", null, SexoAnimal.HEMBRA, null, false, razaId,
                categoriaId, null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO, PROPIEDAD_ID, potreroId, null,
                EstadoAnimal.ACTIVO, LocalDate.of(2025, 1, 1), null, null, null, null, null, 0);
    }

    private UserContext userContext() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), null, UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        return new UserContext(() -> currentUser);
    }

    private JdbcClient jdbcClient(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("control-ecto-test.db") + "?foreign_keys=on");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        return JdbcClient.create(dataSource);
    }
}
