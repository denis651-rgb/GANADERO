package bo.com.ganadero.movimientos.infrastructure;

import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcMovimientoRepositoryTest {
    @Test
    void historialPorLoteIncluyeCabeceraYDetallesSinDuplicadosYPaginaCorrectamente(@TempDir Path dir) {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + dir.resolve("movimientos.db") + "?foreign_keys=on");
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(ds);
        JdbcMovimientoRepository repo = new JdbcMovimientoRepository(jdbc);
        UUID lote = crearLote(jdbc);
        UUID otroLote = crearLote(jdbc);
        UUID traslado = crearMovimiento(jdbc, lote, lote, "CONFIRMADO");
        UUID mixto = crearMovimiento(jdbc, null, null, "REVERTIDO");
        UUID entrada = crearMovimiento(jdbc, otroLote, lote, "CONFIRMADO");
        crearMovimiento(jdbc, otroLote, otroLote, "CONFIRMADO");
        crearDetalle(jdbc, mixto, lote, null);
        crearDetalle(jdbc, mixto, lote, null);
        crearDetalle(jdbc, traslado, lote, lote);

        var todos = repo.findAll(UUID.randomUUID(), null, null, lote, 0, 10);
        assertThat(todos.totalElements()).isEqualTo(3);
        assertThat(todos.content()).extracting(Movimiento::id).containsExactlyInAnyOrder(traslado, mixto, entrada);
        var filtrados = repo.findAll(UUID.randomUUID(), EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_POTRERO, lote, 0, 1);
        assertThat(filtrados.totalElements()).isEqualTo(2);
        assertThat(filtrados.totalPages()).isEqualTo(2);
        var segunda = repo.findAll(UUID.randomUUID(), EstadoMovimiento.CONFIRMADO, TipoMovimiento.CAMBIO_POTRERO, lote, 1, 1);
        assertThat(segunda.content()).hasSize(1);
        assertThat(segunda.content().getFirst().id()).isNotEqualTo(filtrados.content().getFirst().id());
        assertThat(repo.findAll(UUID.randomUUID(), null, null, 0, 10).totalElements()).isEqualTo(4);
    }

    private UUID crearLote(JdbcClient jdbc) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into lote_ganadero(id,codigo,nombre) values(:id,:id,'Lote')").param("id", id.toString()).update();
        return id;
    }

    private UUID crearMovimiento(JdbcClient jdbc, UUID origen, UUID destino, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into movimiento(id,tipo,estado,fecha_movimiento,origen_lote_id,destino_lote_id) "
                + "values(:id,'CAMBIO_POTRERO',:estado,'2026-09-11',:origen,:destino)")
                .param("id", id.toString()).param("estado", estado)
                .param("origen", origen == null ? null : origen.toString())
                .param("destino", destino == null ? null : destino.toString()).update();
        return id;
    }

    private void crearDetalle(JdbcClient jdbc, UUID movimiento, UUID antes, UUID despues) {
        String animal = UUID.randomUUID().toString();
        String potrero = UUID.randomUUID().toString();
        jdbc.sql("insert into potrero(id,codigo,nombre) values(:id,:id,'Potrero')").param("id", potrero).update();
        jdbc.sql("""
                insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,
                    propiedad_actual_id,potrero_actual_id,estado,fecha_ingreso)
                values(:id,:id,'HEMBRA','50000000-0000-0000-0000-000000000001',
                    '60000000-0000-0000-0000-000000000001','CARNE','COMPRADO',
                    '00000000-0000-0000-0000-000000000001',:potrero,'ACTIVO','2026-09-01')
                """).param("id", animal).param("potrero", potrero).update();
        jdbc.sql("insert into movimiento_detalle(id,movimiento_id,animal_id,lote_antes,lote_despues) "
                + "values(:id,:movimiento,:animal,:antes,:despues)")
                .param("id", UUID.randomUUID().toString()).param("movimiento", movimiento.toString())
                .param("animal", animal).param("antes", antes.toString())
                .param("despues", despues == null ? null : despues.toString()).update();
    }
}
