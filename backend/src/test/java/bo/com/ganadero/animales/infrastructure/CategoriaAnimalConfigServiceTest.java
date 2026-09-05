package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.application.CategoriaAnimalConfigService;
import bo.com.ganadero.animales.application.RangoCategoriaCommand;
import bo.com.ganadero.animales.application.ResultadoReclasificacion;
import bo.com.ganadero.animales.domain.CategoriaAnimal;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Corre contra SQLite real (con Flyway aplicado): las validaciones de solape/hueco dependen de
 * los rangos ya sembrados en V1/V14, y la reclasificación depende de transacciones reales por
 * animal. Vive en infrastructure porque Jdbc* son package-private.
 */
class CategoriaAnimalConfigServiceTest {
    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void rechazaRangoSolapadoConUnaCategoriaAutomaticaExistente(@TempDir Path tempDir) {
        CategoriaAnimalConfigService service = servicio(tempDir).service();

        // TORO (MACHO) quedó en 36+ meses tras V14; 20-40 se superpone.
        RangoCategoriaCommand solapado = new RangoCategoriaCommand("PROBADOR", "Probador", "MACHO", 20, 40, null, true, 0, false);

        assertThatThrownBy(() -> service.crear(solapado))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_CATEGORY_RANGE_OVERLAP));
    }

    @Test
    void rechazaHuecoSinConfirmarYLoPermiteConConfirmacionExplicita(@TempDir Path tempDir) {
        CategoriaAnimalConfigService service = servicio(tempDir).service();
        UUID vaquillaId = UUID.fromString("60000000-0000-0000-0000-000000000003");

        // Al desactivar Vaquilla (13-35 HEMBRA) queda un hueco entre Ternera (0-12) y Vaca (36+).
        service.cambiarEstado(vaquillaId, false);

        RangoCategoriaCommand cubreParteDelHueco = new RangoCategoriaCommand("PARCIAL", "Parcial", "HEMBRA", 13, 20, null, true, 0, false);
        assertThatThrownBy(() -> service.crear(cubreParteDelHueco))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_CATEGORY_RANGE_GAP));

        // Con confirmación explícita, el hueco parcial restante (21-35) es intencional y se permite.
        CategoriaAnimal creada = service.crear(new RangoCategoriaCommand("PARCIAL", "Parcial", "HEMBRA", 13, 20, null, true, 0, true));
        assertThat(creada.edadMinMeses()).isEqualTo(13);

        // Cerrar el resto del hueco (21-35) sin confirmación explícita ya no debe fallar.
        CategoriaAnimal resto = service.crear(new RangoCategoriaCommand("RESTO", "Resto", "HEMBRA", 21, 35, null, true, 0, false));
        assertThat(resto.edadMaxMeses()).isEqualTo(35);
    }

    @Test
    void permiteSoloUnRangoAbiertoPorSexo(@TempDir Path tempDir) {
        CategoriaAnimalConfigService service = servicio(tempDir).service();

        // MACHO ya tiene un rango abierto (Toro, 36+); otro rango abierto debe rechazarse aunque
        // no se superponga en el papel (ambos "hasta el infinito").
        RangoCategoriaCommand otroAbierto = new RangoCategoriaCommand("MACHO2", "Otro abierto", "MACHO", 100, null, null, true, 0, true);

        assertThatThrownBy(() -> service.crear(otroAbierto))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_CATEGORY_RANGE_MULTIPLE_OPEN));
    }

    @Test
    void noPermiteEliminarUnaCategoriaConHistorialYSimularNoPersiste(@TempDir Path tempDir) {
        Fixture f = servicio(tempDir);
        CategoriaAnimalConfigService service = f.service();

        // clasificacionAutomatica=false: categoría de excepción manual, exenta de solape/hueco —
        // aquí solo interesa probar el bloqueo de borrado y que simular no persiste.
        CategoriaAnimal nueva = service.crear(new RangoCategoriaCommand("EXTRA", "Extra", "HEMBRA", 200, 210, null, false, 0, true));
        long antesDeSimular = f.jdbc().sql("select count(*) from categoria_animal").query(Integer.class).single();
        int afectados = service.simular(new RangoCategoriaCommand("EXTRA", "Extra", "HEMBRA", 200, 210, null, false, 0, true), nueva.id());
        assertThat(afectados).isZero();
        assertThat(f.jdbc().sql("select count(*) from categoria_animal").query(Integer.class).single()).isEqualTo(antesDeSimular);

        UUID razaId = UUID.randomUUID();
        f.jdbc().sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')").param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID potreroId = UUID.randomUUID();
        f.jdbc().sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Potrero',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID animalId = UUID.randomUUID();
        f.jdbc().sql("insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,estado,fecha_ingreso) " +
                "values(:id,'HIST-1','HEMBRA',:raza,:cat,'CARNE','COMPRADO',:pot,'ACTIVO',date('now'))")
                .param("id", animalId.toString()).param("raza", razaId.toString()).param("cat", nueva.id().toString()).param("pot", potreroId.toString()).update();
        f.jdbc().sql("insert into historial_categoria_animal(id,animal_id,categoria_nueva_id,tipo_cambio) values(:id,:animal,:cat,'MANUAL')")
                .param("id", UUID.randomUUID().toString()).param("animal", animalId.toString()).param("cat", nueva.id().toString()).update();

        assertThatThrownBy(() -> service.eliminar(nueva.id()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_CATEGORY_IN_USE));
    }

    @Test
    void reclasificarEsIdempotenteYRegistraHistorial(@TempDir Path tempDir) {
        Fixture f = servicio(tempDir);
        CategoriaAnimalConfigService service = f.service();

        UUID razaId = UUID.randomUUID();
        f.jdbc().sql("insert into raza(id,codigo,nombre) values(:id,:c,'Brahman')").param("id", razaId.toString()).param("c", "RAZA-" + razaId).update();
        UUID potreroId = UUID.randomUUID();
        f.jdbc().sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values(:id,:c,'Potrero',:p,1)")
                .param("id", potreroId.toString()).param("c", "POT-" + potreroId).param("p", PROPIEDAD_ID.toString()).update();
        UUID terneraId = UUID.fromString("60000000-0000-0000-0000-000000000002");
        UUID vaquillaId = UUID.fromString("60000000-0000-0000-0000-000000000003");
        UUID animalId = UUID.randomUUID();
        // Nace hace 20 meses: le corresponde Vaquilla (13-35), pero queda mal cargado como Ternera.
        f.jdbc().sql("insert into animal(id,codigo,sexo,fecha_nacimiento,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,estado,fecha_ingreso) " +
                "values(:id,:c,'HEMBRA',date('now','-20 months'),:raza,:cat,'CARNE','NACIDO',:pot,'ACTIVO',date('now','-20 months'))")
                .param("id", animalId.toString()).param("c", "RECLAS-1").param("raza", razaId.toString())
                .param("cat", terneraId.toString()).param("pot", potreroId.toString()).update();

        ResultadoReclasificacion primero = service.reclasificarSistema();
        assertThat(primero.actualizados()).isEqualTo(1);
        assertThat(f.jdbc().sql("select categoria_actual_id from animal where id=:id").param("id", animalId.toString()).query(String.class).single())
                .isEqualTo(vaquillaId.toString());
        assertThat(f.jdbc().sql("select count(*) from historial_categoria_animal where animal_id=:id").param("id", animalId.toString()).query(Integer.class).single())
                .isEqualTo(1);

        ResultadoReclasificacion segundo = service.reclasificarSistema();
        assertThat(segundo.actualizados()).isZero();
        assertThat(f.jdbc().sql("select count(*) from historial_categoria_animal where animal_id=:id").param("id", animalId.toString()).query(Integer.class).single())
                .isEqualTo(1);
    }

    private Fixture servicio(Path tempDir) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(), true);
        UserContext context = new UserContext(() -> currentUser);
        CategoriaAnimalConfigService service = new CategoriaAnimalConfigService(new JdbcCategoriaAnimalRepository(jdbc),
                new JdbcAnimalRepository(jdbc), new JdbcHistorialCategoriaAnimalRepository(jdbc), context,
                new DataSourceTransactionManager(dataSource));
        return new Fixture(jdbc, service);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("categorias-test.db") + "?foreign_keys=on");
        return dataSource;
    }

    private record Fixture(JdbcClient jdbc, CategoriaAnimalConfigService service) {
    }
}
