package bo.com.ganadero.lotes.infrastructure;

import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.shared.error.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class LoteCapacidadIntegrationTest {
    private static final UUID PROPERTY = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void bloqueaExcesoLiberaCuposYConservaHistorial(@TempDir Path dir) {
        var f = fixture(dir);
        var lote = crear(f, 1);
        var a = animal(f);
        var b = animal(f);
        ingresar(f, lote.id(), a);
        assertThat(f.repo.findById(lote.id(), null).orElseThrow().cantidadActual()).isEqualTo(1);
        assertThatThrownBy(() -> ingresar(f, lote.id(), b)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.LOT_CAPACITY_EXCEEDED));
        f.repo.closeMembership(lote.id(), null, a, null, "Salida", Instant.now(), ACTOR);
        ingresar(f, lote.id(), b);
        assertThat(f.repo.findMemberships(lote.id(), null, false)).hasSize(2);
        assertThat(f.repo.findMemberships(lote.id(), null, true)).hasSize(1);
        assertThat(f.repo.findAll(null, Set.of(), true, null, null, 0, 10).content())
                .anySatisfy(l -> { assertThat(l.cantidadMaxima()).isEqualTo(1); assertThat(l.cantidadActual()).isEqualTo(1); });
        assertThat(f.repo.findActiveLotOfAnimal(b, null).orElseThrow().cantidadActual()).isEqualTo(1);
    }

    @Test
    void revierteTodaLaTransaccionSiUnIngresoSuperaElLimite(@TempDir Path dir) {
        var f = fixture(dir);
        var lote = crear(f, 1);
        var a = animal(f);
        var b = animal(f);
        assertThatThrownBy(() -> f.tx.execute(status -> {
            ingresar(f, lote.id(), a);
            ingresar(f, lote.id(), b);
            return null;
        })).isInstanceOf(BusinessException.class);
        assertThat(f.repo.findMemberships(lote.id(), null, false)).isEmpty();
    }

    @Test
    void noPermiteReducirPorDebajoDeOcupacion(@TempDir Path dir) {
        var f = fixture(dir);
        var lote = crear(f, null);
        ingresar(f, lote.id(), animal(f));
        ingresar(f, lote.id(), animal(f));
        var cambio = new Lote(lote.id(), null, PROPERTY, lote.codigo(), lote.nombre(), null,
                EstadoLote.ACTIVO, lote.fechaApertura(), null, lote.version(), 1, 2);
        assertThatThrownBy(() -> f.repo.update(cambio, ACTOR)).isInstanceOf(BusinessException.class);
        assertThat(f.repo.findById(lote.id(), null).orElseThrow().cantidadMaxima()).isNull();
    }

    @Test
    void dosSolicitudesNoPuedenOcuparElUltimoCupo(@TempDir Path dir) throws Exception {
        var f = fixture(dir);
        var lote = crear(f, 1);
        var a = animal(f);
        var b = animal(f);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (UUID id : List.of(a, b)) results.add(executor.submit(() -> {
                start.await();
                try {
                    f.tx.execute(status -> { ingresar(f, lote.id(), id); return null; });
                    return true;
                } catch (BusinessException | org.springframework.dao.DataAccessException ex) { return false; }
            }));
            start.countDown();
            int success = 0;
            for (var result : results) if (result.get(10, TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(1);
        }
        assertThat(f.repo.findById(lote.id(), null).orElseThrow().cantidadActual()).isEqualTo(1);
    }

    private void ingresar(Fixture f, UUID lote, UUID animal) {
        f.repo.openMembership(lote, null, animal, null, "Prueba", null, "ATOMICO", Instant.now(), ACTOR);
    }

    private Lote crear(Fixture f, Integer maximo) {
        var id = UUID.randomUUID();
        return f.repo.create(new Lote(id, null, PROPERTY, id.toString(), "Lote de prueba", null,
                EstadoLote.ACTIVO, LocalDate.now(), null, 0, maximo, 0), ACTOR);
    }

    private UUID animal(Fixture f) {
        var id = UUID.randomUUID();
        f.jdbc.sql("""
                insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,
                potrero_actual_id,propiedad_actual_id,fecha_ingreso)
                values(:id,:id,'MACHO',(select id from raza limit 1),(select id from categoria_animal limit 1),
                'CARNE','COMPRADO',:potrero,:propiedad,'2026-09-03')""")
                .param("id", id.toString()).param("potrero", f.potrero.toString()).param("propiedad", PROPERTY.toString()).update();
        return id;
    }

    private Fixture fixture(Path dir) {
        return fixture(dir, "10");
    }

    @Test
    void recuperaSoloVentaConfirmadaYConservaNombreDelVendido(@TempDir Path dir) {
        var f = fixture(dir, "9");
        var lote = crear(f, 3);
        var vendido = animal(f);
        var activo = animal(f);
        var pendiente = animal(f);
        String confirmacion = Instant.now().plusSeconds(10).toString();
        for (var id : List.of(vendido, activo, pendiente)) {
            ingresar(f, lote.id(), id);
            f.jdbc.sql("update animal set lote_actual_id=:lote,estado=:estado,nombre='LB vendido' where id=:id")
                    .param("lote", lote.id().toString()).param("id", id.toString())
                    .param("estado", id.equals(activo) ? "ACTIVO" : "VENDIDO").update();
            var movimiento = UUID.randomUUID().toString();
            f.jdbc.sql("""
                    insert into movimiento(id,tipo,estado,fecha_movimiento,fecha_confirmacion,usuario_confirma)
                    values(:id,'SALIDA_VENTA',:estado,'2026-09-03',:fecha,:actor)""")
                    .param("id", movimiento).param("estado", id.equals(pendiente) ? "PENDIENTE" : "CONFIRMADO")
                    .param("fecha", confirmacion).param("actor", ACTOR.toString()).update();
            f.jdbc.sql("""
                    insert into movimiento_detalle(id,movimiento_id,animal_id,lote_antes,lote_despues)
                    values(:id,:movimiento,:animal,:lote,:lote)""")
                    .param("id", UUID.randomUUID().toString()).param("movimiento", movimiento)
                    .param("animal", id.toString()).param("lote", lote.id().toString()).update();
        }
        Flyway.configure().dataSource(f.ds).locations("classpath:db/migration").mixed(true).load().migrate();
        var salida = f.repo.findMemberships(lote.id(), null, false).stream()
                .filter(m -> m.animalId().equals(vendido)).findFirst().orElseThrow();
        assertThat(salida.fechaSalida()).isEqualTo(Instant.parse(confirmacion));
        assertThat(salida.motivoSalida()).isEqualTo("Movimiento SALIDA_VENTA");
        assertThat(salida.animalNombre()).isEqualTo("LB vendido");
        assertThat(salida.animalCodigo()).isEqualTo(vendido.toString());
        assertThat(salida.observacion()).contains("Salida recuperada");
        assertThat(f.repo.findMemberships(lote.id(), null, true)).hasSize(2);
        assertThat(f.repo.findById(lote.id(), null).orElseThrow().cantidadActual()).isEqualTo(2);
        assertThat(f.jdbc.sql("select count(*) from animal where id=:id and lote_actual_id is null")
                .param("id", vendido.toString()).query(Integer.class).single()).isEqualTo(1);
        Flyway.configure().dataSource(f.ds).locations("classpath:db/migration").mixed(true).load().migrate();
        assertThat(f.repo.findMemberships(lote.id(), null, false)).hasSize(3);
    }

    private Fixture fixture(Path dir, String target) {
        var ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + dir.resolve("lotes.db") + "?foreign_keys=on&busy_timeout=5000");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).target(target).load().migrate();
        var jdbc = JdbcClient.create(ds);
        var potrero = UUID.randomUUID();
        jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id) values(:id,:id,'Prueba',:propiedad)")
                .param("id", potrero.toString()).param("propiedad", PROPERTY.toString()).update();
        return new Fixture(jdbc, new JdbcLoteRepository(jdbc), new TransactionTemplate(new DataSourceTransactionManager(ds)), potrero, ds);
    }
    private record Fixture(JdbcClient jdbc, JdbcLoteRepository repo, TransactionTemplate tx, UUID potrero, SQLiteDataSource ds) {}
}
