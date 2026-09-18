package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.AplicacionTratamiento;
import bo.com.ganadero.sanidad.domain.EstadoAplicacionTratamiento;
import bo.com.ganadero.sanidad.domain.EventoCalendarioSanitarioRepository;
import bo.com.ganadero.sanidad.domain.SanidadRepository;
import bo.com.ganadero.sanidad.domain.Tratamiento;
import bo.com.ganadero.sanidad.domain.TratamientoDetalle;
import bo.com.ganadero.sanidad.infrastructure.JdbcClinicaRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Protocolo de tratamiento: la fecha fin estimada debe alcanzar la última dosis programada y el
 * producto aplicado queda registrado como texto (no hay inventario en el sistema).
 */
class CrearTratamientoServiceTest {

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");

    @Test
    void guardaElProductoAplicadoEnElDetalle(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal vaca = sembrarAnimal(jdbc);
        ClinicaService service = service(repo, mockAnimalRepository(vaca));
        LocalDate inicio = LocalDate.now(BOLIVIA);

        Tratamiento tratamiento = service.crearTratamiento(command(vaca.id(), inicio, inicio.plusDays(4), 5, "Oxitetraciclina LA"));

        List<TratamientoDetalle> detalles = repo.detalles(tratamiento.id(), null);
        assertThat(detalles).hasSize(1);
        assertThat(detalles.get(0).productoTexto()).isEqualTo("Oxitetraciclina LA");
    }

    @Test
    void rechazaFechaFinAnteriorALaUltimaDosis(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal vaca = sembrarAnimal(jdbc);
        ClinicaService service = service(repo, mockAnimalRepository(vaca));
        LocalDate inicio = LocalDate.now(BOLIVIA);

        assertThatThrownBy(() -> service.crearTratamiento(command(vaca.id(), inicio, inicio.plusDays(4), 10, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("última dosis programada");
    }

    @Test
    void regenerarReprogramaDosisRestantesDesdeAhora(@TempDir Path tempDir) {
        JdbcClient jdbc = jdbcClient(tempDir);
        JdbcClinicaRepository repo = new JdbcClinicaRepository(jdbc);
        Animal vaca = sembrarAnimal(jdbc);
        ClinicaService service = service(repo, mockAnimalRepository(vaca));
        LocalDate inicio = LocalDate.now(BOLIVIA).minusDays(2);

        Tratamiento tratamiento = service.crearTratamiento(command(vaca.id(), inicio, inicio.plusDays(4), 5, null));
        service.activar(tratamiento.id());
        List<AplicacionTratamiento> iniciales = repo.aplicaciones(tratamiento.id(), null);
        assertThat(iniciales).hasSize(5);
        for (AplicacionTratamiento aplicacion : iniciales.subList(0, 2)) {
            service.aplicar(tratamiento.id(), aplicacion.id(),
                    new AplicarTratamientoCommand(aplicacion.dosisProgramada(), null, aplicacion.version()));
        }

        Instant antes = Instant.now();
        service.regenerar(tratamiento.id());

        List<AplicacionTratamiento> despues = repo.aplicaciones(tratamiento.id(), null);
        assertThat(despues.stream().filter(a -> a.estado() == EstadoAplicacionTratamiento.APLICADA)).hasSize(2);
        List<AplicacionTratamiento> pendientes = despues.stream()
                .filter(a -> a.estado() == EstadoAplicacionTratamiento.PENDIENTE).toList();
        assertThat(pendientes).hasSize(3);
        assertThat(pendientes).allMatch(a -> !a.fechaProgramada().isBefore(antes));

        Instant ultima = pendientes.stream().map(AplicacionTratamiento::fechaProgramada).max(Instant::compareTo).orElseThrow();
        Instant finEsperado = ultima.atZone(BOLIVIA).toLocalDate().atStartOfDay(BOLIVIA).toInstant();
        assertThat(repo.tratamiento(tratamiento.id(), null).orElseThrow().fechaFinEstimada()).isEqualTo(finEsperado);
    }

    private CrearTratamientoCommand command(UUID animalId, LocalDate inicio, LocalDate fin, int duracion, String producto) {
        return new CrearTratamientoCommand(null, animalId, inicio.atStartOfDay(BOLIVIA).toInstant(),
                fin.atStartOfDay(BOLIVIA).toInstant(), null, null, null,
                List.of(new CrearTratamientoCommand.Detalle(null, null, new BigDecimal("10"), "mL", 24, duracion, "IM", 0, 4, producto)),
                null);
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

    private Animal sembrarAnimal(JdbcClient jdbc) {
        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values(:id,:c,'Vaca','HEMBRA')")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();
        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Servicio',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID animalId = UUID.randomUUID();
        String codigo = "VACA-001";
        jdbc.sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso) "
                + "values(:id,:cod,:sexo,:raza,:cat,'LECHE','NACIDO',:pot,:ingreso)")
                .param("id", animalId.toString()).param("cod", codigo).param("sexo", SexoAnimal.HEMBRA.name())
                .param("raza", razaId.toString()).param("cat", categoriaId.toString())
                .param("pot", potreroId.toString()).param("ingreso", LocalDate.now(BOLIVIA).minusDays(30).toString()).update();
        return new Animal(animalId, null, codigo, null, SexoAnimal.HEMBRA, null, false, razaId,
                categoriaId, null, PropositoAnimal.LECHE, OrigenAnimal.NACIDO, PROPIEDAD_ID, potreroId, null,
                EstadoAnimal.ACTIVO, LocalDate.of(2025, 1, 1), null, null, null, null, null, 0);
    }

    private UserContext userContext() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), null, UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        return new UserContext(() -> currentUser);
    }

    private JdbcClient jdbcClient(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("tratamiento-test.db") + "?foreign_keys=on");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        return JdbcClient.create(dataSource);
    }
}
