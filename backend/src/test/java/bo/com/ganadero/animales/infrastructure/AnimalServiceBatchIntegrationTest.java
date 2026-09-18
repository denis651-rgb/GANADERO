package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.application.AnimalCommand;
import bo.com.ganadero.animales.application.AnimalService;
import bo.com.ganadero.animales.api.CrearAnimalesLoteRequest;
import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.TimelineService;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Fase 2 del plan sanitario (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 7): alta
 * masiva de animales comprados. Corre contra SQLite real (con Flyway aplicado) y una
 * transaccion real (DataSourceTransactionManager), porque lo que hay que probar es
 * precisamente el comportamiento transaccional de AnimalService.createBatch — algo que un
 * repositorio mockeado no puede demostrar. Vive en el paquete infrastructure porque
 * JdbcAnimalRepository/JdbcRazaRepository/JdbcCategoriaAnimalRepository son package-private.
 */
class AnimalServiceBatchIntegrationTest {

    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void creaVariosAnimalesComoCompradosEnLaMismaTransaccion(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        List<AnimalCommand> comandos = List.of(
                comando(f, "LOTE-001"), comando(f, "LOTE-002"), comando(f, "LOTE-003"));

        List<Animal> creados = f.tx().execute(status -> f.service().createBatch(comandos));

        assertThat(creados).hasSize(3);
        assertThat(creados).allSatisfy(a -> assertThat(a.origen()).isEqualTo(OrigenAnimal.COMPRADO));
        assertThat(creados).extracting(Animal::codigo).containsExactly("LOTE-001", "LOTE-002", "LOTE-003");
        assertThat(f.jdbc().sql("select count(*) as n from animal").query(Integer.class).single()).isEqualTo(3);
    }

    @Test
    void unAnimalNacidoIngresaAlHatoEnSuFechaDeNacimiento(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        LocalDate nacimiento = LocalDate.of(2026, 8, 20);
        AnimalCommand comando = new AnimalCommand(null, "NACIDO-001", "Cria", SexoAnimal.HEMBRA,
                nacimiento, false, f.razaId(), f.categoriaId(), null, PropositoAnimal.CARNE,
                OrigenAnimal.NACIDO, PROPIEDAD_ID, f.potreroId(), null, LocalDate.now(java.time.ZoneId.of("America/La_Paz")),
                null, null, null, null, null, 0L);

        Animal creado = f.tx().execute(status -> f.service().create(comando));

        assertThat(creado.fechaIngreso()).isEqualTo(nacimiento);
        assertThat(f.jdbc().sql("select fecha_ingreso from animal where id=:id")
                .param("id", creado.id().toString()).query(String.class).single())
                .isEqualTo(nacimiento.toString());
    }

    @Test
    void unCodigoDuplicadoEnElLoteRevierteTodaLaTransaccion(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        List<AnimalCommand> comandos = List.of(
                comando(f, "LOTE-010"), comando(f, "LOTE-011"), comando(f, "LOTE-010"));

        assertThatThrownBy(() -> f.tx().execute(status -> f.service().createBatch(comandos)))
                .isInstanceOf(RuntimeException.class);

        assertThat(f.jdbc().sql("select count(*) as n from animal").query(Integer.class).single()).isZero();
    }

    @Test
    void elPesoDeCompraNuncaSeGuardaComoPesoAlNacer(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        var request = new CrearAnimalesLoteRequest(f.razaId(), PropositoAnimal.CARNE,
                PROPIEDAD_ID, f.potreroId(), LocalDate.of(2026, 9, 3), null, List.of(
                new CrearAnimalesLoteRequest.AnimalLoteItemRequest(null, "Estimado", SexoAnimal.HEMBRA,
                        f.categoriaId(), null, null, true, new BigDecimal("150"), true, null, null),
                new CrearAnimalesLoteRequest.AnimalLoteItemRequest(null, "Medido", SexoAnimal.HEMBRA,
                        f.categoriaId(), null, LocalDate.of(2025, 9, 3), true, new BigDecimal("180"), false, null, null)));
        var creados = f.tx().execute(status -> f.service().createBatch(request.commands()));
        assertThat(creados).allSatisfy(a -> assertThat(a.pesoNacimientoKg()).isNull());
        assertThat(creados.get(0).pesoIngresoKg()).isEqualByComparingTo("150");
        assertThat(creados.get(0).pesoIngresoEstimado()).isTrue();
        assertThat(creados.get(0).fechaNacimiento()).isNull();
        assertThat(creados.get(0).fechaNacimientoEstimada()).isFalse();
        assertThat(creados.get(1).pesoIngresoEstimado()).isFalse();
        assertThat(creados.get(1).fechaNacimientoEstimada()).isTrue();
        assertThat(f.timeline().get(0).metadata()).containsEntry("pesoIngresoEstimado", true);
        assertThat(f.timeline().get(1).metadata()).containsEntry("pesoIngresoEstimado", false);
    }

    @Test
    void corrigeSoloElPesoConfirmadoYPermiteNacimientoDesconocido(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        Animal original = f.tx().execute(status -> f.service().create(comando(f, "ANTIGUO")));
        Animal otro = f.tx().execute(status -> f.service().create(comando(f, "OTRO")));
        f.jdbc().sql("update animal set peso_nacimiento_kg=150,fecha_nacimiento='2025-09-03',fecha_nacimiento_estimada=1").update();
        AnimalCommand correccion = new AnimalCommand(null, null, null, null, null, false,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, original.version(), new BigDecimal("150"), true, true, true);
        Animal guardado = f.tx().execute(status -> f.service().update(original.id(), correccion));
        assertThat(guardado.pesoNacimientoKg()).isNull();
        assertThat(guardado.pesoIngresoKg()).isEqualByComparingTo("150");
        assertThat(guardado.pesoIngresoEstimado()).isTrue();
        assertThat(guardado.fechaNacimiento()).isNull();
        assertThat(guardado.fechaNacimientoEstimada()).isFalse();
        assertThat(f.timeline().get(2).metadata()).containsEntry("correccionPesoCompraConfirmada", true)
                .containsEntry("nacimientoDesconocido", true);
        assertThat((BigDecimal) f.timeline().get(2).metadata().get("pesoNacimientoAnteriorKg")).isEqualByComparingTo("150");
        assertThat(f.service().get(otro.id()).pesoNacimientoKg()).isEqualByComparingTo("150");
        assertThatThrownBy(() -> f.tx().execute(status -> f.service().update(original.id(), correccion)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rechazaUnPesoSinIndicarSiEsEstimadoOMedido(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        var request = new CrearAnimalesLoteRequest(f.razaId(), PropositoAnimal.CARNE,
                PROPIEDAD_ID, f.potreroId(), LocalDate.now(java.time.ZoneId.of("America/La_Paz")), null, List.of(
                new CrearAnimalesLoteRequest.AnimalLoteItemRequest(null, null, SexoAnimal.HEMBRA,
                        f.categoriaId(), null, null, false, new BigDecimal("150"), null, null, null)));
        assertThatThrownBy(() -> f.tx().execute(status -> f.service().createBatch(request.commands())))
                .isInstanceOf(RuntimeException.class);
        assertThat(f.jdbc().sql("select count(*) from animal").query(Integer.class).single()).isZero();
    }

    @Test
    void filtraPorPropiedadAlListarAnimales(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID otraPropiedadId = UUID.randomUUID();
        UUID otroPotreroId = UUID.randomUUID();
        f.jdbc().sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Otro potrero',:p,1)")
                .param("id", otroPotreroId.toString()).param("c", "POT-" + otroPotreroId).param("p", otraPropiedadId.toString()).update();

        f.tx().execute(status -> f.service().create(comando(f, "AQUI-001")));
        AnimalCommand comandoOtraPropiedad = new AnimalCommand(null, "ALLA-001", null, SexoAnimal.HEMBRA, null, false,
                f.razaId(), f.categoriaId(), null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, otraPropiedadId, otroPotreroId, null,
                LocalDate.now(java.time.ZoneId.of("America/La_Paz")), new BigDecimal("2500"), null, null, null, "Compra de prueba", 0L);
        f.tx().execute(status -> f.service().create(comandoOtraPropiedad));

        JdbcAnimalRepository repo = new JdbcAnimalRepository(f.jdbc());
        AnimalFilter filtro = new AnimalFilter(null, PROPIEDAD_ID, null, null, null, null, null, 0, 20);
        AnimalPage pagina = repo.findAll(null, Set.of(), filtro);

        assertThat(pagina.content()).extracting(Animal::codigo).containsExactly("AQUI-001");
    }

    private AnimalCommand comando(Fixture f, String codigo) {
        return new AnimalCommand(null, codigo, null, SexoAnimal.HEMBRA, null, false, f.razaId(), f.categoriaId(),
                null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, PROPIEDAD_ID, f.potreroId(), null,
                LocalDate.now(java.time.ZoneId.of("America/La_Paz")), new BigDecimal("2500"), null, null, null, "Compra de prueba", 0L);
    }

    private Fixture fixture(Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);

        UUID razaId = UUID.randomUUID();
        jdbc.sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')")
                .param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();

        // clasificacion_automatica=0: sin rango de edad, esta categoría de prueba se asigna manualmente
        // (si fuera automática, competiría en cualquier edad con las categorías reales sembradas en V1).
        UUID categoriaId = UUID.randomUUID();
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable,clasificacion_automatica) values(:id,:c,'Vaquillona','AMBOS',0)")
                .param("id", categoriaId.toString()).param("c", "CAT-" + categoriaId).update();

        UUID potreroId = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Potrero de ingreso',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();

        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        UserContext context = new UserContext(() -> currentUser);
        List<RegistrarEventoTimeline> timeline = new ArrayList<>();
        AnimalService service = new AnimalService(new JdbcAnimalRepository(jdbc), new JdbcRazaRepository(jdbc),
                new JdbcCategoriaAnimalRepository(jdbc), new JdbcHistorialCategoriaAnimalRepository(jdbc), context,
                mock(ApplicationEventPublisher.class), timeline::add, mock(TimelineService.class), new CodigoService(jdbc));

        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        return new Fixture(jdbc, service, tx, razaId, categoriaId, potreroId, timeline);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("animales-test.db") + "?foreign_keys=on");
        return dataSource;
    }

    private record Fixture(JdbcClient jdbc, AnimalService service, TransactionTemplate tx,
                            UUID razaId, UUID categoriaId, UUID potreroId, List<RegistrarEventoTimeline> timeline) {
    }
}
