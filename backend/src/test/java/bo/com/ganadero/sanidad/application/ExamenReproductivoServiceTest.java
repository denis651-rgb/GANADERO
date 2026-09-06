package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.EnfermedadReproductiva;
import bo.com.ganadero.sanidad.domain.ExamenReproductivo;
import bo.com.ganadero.sanidad.domain.ResultadoExamenReproductivo;
import bo.com.ganadero.sanidad.domain.ResultadoPruebaReproductiva;
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
import java.math.BigDecimal;
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
 * Fase 5 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 4.e):
 * examen reproductivo de toros y vaquillas antes del servicio. El sexo del animal
 * determina qué campos son válidos — es lo que distingue esta prueba de
 * ControlNeonatalServiceTest / ControlEctoparasitarioServiceTest.
 */
class ExamenReproductivoServiceTest {

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void toroConCamposDeToroYChecklistDeTresEnfermedadesSeGuardaCorrectamente(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal toro = sembrarAnimal(jdbc, SexoAnimal.MACHO);
        ClinicaService service = service(repo, mockAnimalRepository(toro));

        ExamenReproductivo examen = service.crearExamen(new CrearExamenReproductivoCommand(
                toro.id(), LocalDate.of(2026, 1, 1), ResultadoExamenReproductivo.APTO, null,
                new BigDecimal("34"), new BigDecimal("70"), new BigDecimal("80"), "ALTA", "BUENA",
                null, null, null, null, null,
                List.of(
                        new CrearExamenReproductivoCommand.Prueba(EnfermedadReproductiva.IBR, ResultadoPruebaReproductiva.NEGATIVO),
                        new CrearExamenReproductivoCommand.Prueba(EnfermedadReproductiva.BVD, ResultadoPruebaReproductiva.NEGATIVO),
                        new CrearExamenReproductivoCommand.Prueba(EnfermedadReproductiva.BRUCELOSIS, ResultadoPruebaReproductiva.NO_REALIZADO))));

        assertThat(examen.id()).isNotNull();
        assertThat(examen.circunferenciaEscrotalCm()).isEqualByComparingTo("34");
        assertThat(examen.pruebas()).hasSize(3);

        List<ExamenReproductivo> releidos = service.examenesReproductivos(toro.id());
        assertThat(releidos).hasSize(1);
        assertThat(releidos.get(0).pruebas()).hasSize(3)
                .extracting(p -> p.enfermedad())
                .containsExactlyInAnyOrder(EnfermedadReproductiva.IBR, EnfermedadReproductiva.BVD, EnfermedadReproductiva.BRUCELOSIS);
    }

    @Test
    void toroConCampoDeVaquillaFallaConCampoNoAplicable(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal toro = sembrarAnimal(jdbc, SexoAnimal.MACHO);
        ClinicaService service = service(repo, mockAnimalRepository(toro));

        assertThatThrownBy(() -> service.crearExamen(new CrearExamenReproductivoCommand(
                toro.id(), LocalDate.of(2026, 1, 1), ResultadoExamenReproductivo.APTO, null,
                new BigDecimal("34"), null, null, null, null,
                null, new BigDecimal("80"), null, null, null, List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.SANIDAD_EXAMEN_CAMPO_NO_APLICABLE);
    }

    @Test
    void vaquillaConCamposDeVaquillaSeGuardaCorrectamente(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal vaquilla = sembrarAnimal(jdbc, SexoAnimal.HEMBRA);
        ClinicaService service = service(repo, mockAnimalRepository(vaquilla));

        ExamenReproductivo examen = service.crearExamen(new CrearExamenReproductivoCommand(
                vaquilla.id(), LocalDate.of(2026, 1, 1), ResultadoExamenReproductivo.APTO, null,
                null, null, null, null, null,
                new BigDecimal("320"), new BigDecimal("65"), new BigDecimal("3.0"), "ADECUADO",
                null, List.of()));

        assertThat(examen.id()).isNotNull();
        assertThat(examen.pesoKg()).isEqualByComparingTo("320");
        assertThat(examen.condicionCorporal()).isEqualByComparingTo("3.0");
    }

    @Test
    void checklistConEnfermedadRepetidaFallaConPruebaDuplicada(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal toro = sembrarAnimal(jdbc, SexoAnimal.MACHO);
        ClinicaService service = service(repo, mockAnimalRepository(toro));

        assertThatThrownBy(() -> service.crearExamen(new CrearExamenReproductivoCommand(
                toro.id(), LocalDate.of(2026, 1, 1), ResultadoExamenReproductivo.OBSERVACION, null,
                null, null, null, null, null, null, null, null, null, null,
                List.of(
                        new CrearExamenReproductivoCommand.Prueba(EnfermedadReproductiva.IBR, ResultadoPruebaReproductiva.NEGATIVO),
                        new CrearExamenReproductivoCommand.Prueba(EnfermedadReproductiva.IBR, ResultadoPruebaReproductiva.POSITIVO)))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.SANIDAD_EXAMEN_PRUEBA_DUPLICADA);
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

    /** animal_id tiene FK real a animal(id): el animal mockeado igual necesita existir en la tabla. */
    private Animal sembrarAnimal(JdbcClient jdbc, SexoAnimal sexo) {
        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values(:id,:c,'Reproductor','AMBOS')")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Servicio',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID animalId = UUID.randomUUID();
        String codigo = sexo == SexoAnimal.MACHO ? "TORO-001" : "VAQ-001";
        jdbc.sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                + "values(:id,:cod,:sexo,:raza,:cat,'CARNE','NACIDO',:pot,:ingreso)")
                .param("id", animalId.toString()).param("cod", codigo).param("sexo", sexo.name())
                .param("raza", razaId.toString()).param("cat", categoriaId.toString())
                .param("pot", potreroId.toString()).param("ingreso", LocalDate.now().toString()).update();
        return new Animal(animalId, null, codigo, null, sexo, null, false, razaId,
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
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("examen-repro-test.db") + "?foreign_keys=on");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        return JdbcClient.create(dataSource);
    }
}
