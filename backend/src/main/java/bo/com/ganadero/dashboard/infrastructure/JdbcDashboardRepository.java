package bo.com.ganadero.dashboard.infrastructure;

import bo.com.ganadero.dashboard.domain.DashboardRepository;
import bo.com.ganadero.dashboard.domain.DashboardResumen;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** App de escritorio de una sola finca: ya no hay multi-propiedad que filtrar (todas/permitidas se ignoran). */
@Repository
public class JdbcDashboardRepository implements DashboardRepository {
    private final JdbcClient jdbc;

    public JdbcDashboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long countAnimales(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from animal where estado='ACTIVO'");
    }

    @Override
    public long countAnimalesEnPotrero(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from animal where estado='ACTIVO' and potrero_actual_id is not null");
    }

    @Override
    public long countLotesActivos(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from lote_ganadero where estado='ACTIVO'");
    }

    @Override
    public long countPotrerosActivos(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from potrero where activo=1");
    }

    @Override
    public Double pesoPromedio(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return jdbc.sql("""
                select avg(p.peso_kg) from pesaje p
                join animal a on a.id=p.animal_id and a.estado='ACTIVO'
                where p.estado='ACTIVO'
                """).query(Double.class).optional().orElse(null);
    }

    @Override
    public Double gananciaDiaria(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return null;
    }

    @Override
    public long countPesajesUltimosDias(UUID empresa, int dias, boolean todas, Set<UUID> permitidas) {
        return jdbc.sql("select count(*) from pesaje where estado='ACTIVO' and fecha >= date('now', '-' || :dias || ' days')")
                .param("dias", dias).query(Long.class).single();
    }

    @Override
    public long countMovimientosUltimosDias(UUID empresa, int dias, boolean todas, Set<UUID> permitidas) {
        Long value = jdbc.sql("select count(*) from movimiento where fecha_movimiento >= date('now', '-' || :dias || ' days')")
                .param("dias", dias).query(Long.class).single();
        return value == null ? 0 : value;
    }

    @Override
    public long countAnimalesSinPesaje(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return jdbc.sql("""
                select count(*) from animal a where a.estado='ACTIVO' and (
                    select max(p.fecha) from pesaje p where p.animal_id=a.id and p.estado='ACTIVO'
                ) is null or julianday('now') - julianday((
                    select max(p.fecha) from pesaje p where p.animal_id=a.id and p.estado='ACTIVO'
                )) > 30
                """).query(Long.class).single();
    }

    @Override
    public long countAnimalesGananciaNegativa(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return 0;
    }

    @Override
    public long countPotrerosInactivos(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from potrero where activo=0");
    }

    @Override
    public List<DashboardResumen.Distribucion> animalesPorCategoria(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return distribucion("""
                select coalesce(c.nombre,'Sin categoría') as nombre, count(*) as total
                from animal a
                left join categoria_animal c on c.id=a.categoria_actual_id
                where a.estado='ACTIVO'
                """);
    }

    @Override
    public List<DashboardResumen.Distribucion> animalesPorPotrero(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return distribucion("""
                select coalesce(p.nombre,'Sin potrero') as nombre, count(*) as total
                from animal a
                left join potrero p on p.id=a.potrero_actual_id
                where a.estado='ACTIVO'
                """);
    }

    @Override
    public List<DashboardResumen.Distribucion> animalesPorLote(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return distribucion("""
                select coalesce(l.nombre,'Sin lote') as nombre, count(*) as total
                from animal a
                left join lote_ganadero l on l.id=a.lote_actual_id
                where a.estado='ACTIVO'
                """);
    }

    @Override
    public List<DashboardResumen.PesajeReciente> pesajesRecientes(UUID empresa, boolean todas, Set<UUID> permitidas, int limite) {
        String sql = """
                select p.id, p.animal_id, a.codigo as animal_codigo, a.nombre as animal_nombre, p.fecha, p.peso_kg
                from pesaje p
                join animal a on a.id=p.animal_id
                where p.estado='ACTIVO'
                order by p.fecha desc, p.created_at desc limit :n
                """;
        return jdbc.sql(sql).param("n", limite).query((rs, rowNum) ->
                new DashboardResumen.PesajeReciente(
                        Rows.uuid(rs, "id"),
                        Rows.uuid(rs, "animal_id"),
                        rs.getString("animal_codigo"),
                        rs.getString("animal_nombre"),
                        Rows.localDate(rs, "fecha"),
                        rs.getBigDecimal("peso_kg"))).list();
    }

    private long count(String sql) {
        Long value = jdbc.sql(sql).query(Long.class).single();
        return value == null ? 0 : value;
    }

    private List<DashboardResumen.Distribucion> distribucion(String base) {
        base += " group by 1 order by total desc, 1";
        return jdbc.sql(base).query((rs, rowNum) ->
                new DashboardResumen.Distribucion(rs.getString("nombre"), rs.getLong("total"))).list();
    }
}
