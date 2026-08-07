package bo.com.ganadero.dashboard.infrastructure;

import bo.com.ganadero.dashboard.domain.DashboardRepository;
import bo.com.ganadero.dashboard.domain.DashboardResumen;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcDashboardRepository implements DashboardRepository {
    private final JdbcClient jdbc;

    public JdbcDashboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long countAnimales(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from ganado.animales where empresa_id=:e and estado='ACTIVO'",
                "propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public long countAnimalesEnPotrero(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from ganado.animales where empresa_id=:e and estado='ACTIVO' " +
                "and potrero_actual_id is not null", "propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public long countLotesActivos(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from ganado.lotes_ganaderos where empresa_id=:e and estado='ACTIVO'",
                "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public long countPotrerosActivos(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from campo.potreros where empresa_id=:e and activo=true",
                "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public Double pesoPromedio(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return scalar("select avg(u.peso_kg) from produccion.v_ultimo_peso_animal u " +
                "join ganado.animales a on a.id=u.animal_id and a.empresa_id=u.empresa_id and a.estado='ACTIVO' " +
                "where u.empresa_id=:e", "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public Double gananciaDiaria(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return scalar("select avg(g.ganancia_diaria_kg) from produccion.v_ganancia_diaria_animal g " +
                "join ganado.animales a on a.id=g.animal_id and a.empresa_id=g.empresa_id and a.estado='ACTIVO' " +
                "where g.empresa_id=:e and g.ganancia_diaria_kg is not null", "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public long countPesajesUltimosDias(UUID empresa, int dias, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from produccion.pesajes where empresa_id=:e and estado='ACTIVO' " +
                "and fecha >= current_date - :dias", "propiedad_id", empresa, todas, permitidas, Map.of("dias", dias));
    }

    @Override
    public long countMovimientosUltimosDias(UUID empresa, int dias, boolean todas, Set<UUID> permitidas) {
        if (!todas && permitidas.isEmpty()) return 0;
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        params.put("dias", dias);
        String sql = "select count(*) from ganado.movimientos where empresa_id=:e " +
                "and fecha_movimiento >= current_date - :dias";
        if (!todas) {
            sql += " and (origen_propiedad_id in (:allowed) or destino_propiedad_id in (:allowed))";
            params.put("allowed", permitidas);
        }
        Long value = jdbc.sql(sql).params(params).query(Long.class).single();
        return value == null ? 0 : value;
    }

    @Override
    public long countAnimalesSinPesaje(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from produccion.v_animales_sin_pesaje where empresa_id=:e and dias_sin_pesaje > 30",
                "propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public long countAnimalesGananciaNegativa(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from produccion.v_ganancia_diaria_animal g " +
                "join ganado.animales a on a.id=g.animal_id and a.empresa_id=g.empresa_id and a.estado='ACTIVO' " +
                "where g.empresa_id=:e and g.ganancia_diaria_kg < 0", "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public long countPotrerosInactivos(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from campo.potreros where empresa_id=:e and activo=false",
                "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public long countLotesCerrados(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from ganado.lotes_ganaderos where empresa_id=:e and estado='CERRADO'",
                "propiedad_id", empresa, todas, permitidas);
    }

    @Override
    public List<DashboardResumen.Distribucion> animalesPorCategoria(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return distribucion("""
                select coalesce(c.nombre,'Sin categoría') as nombre, count(*) as total
                from ganado.animales a
                left join ganado.categorias_animal c on c.id=a.categoria_actual_id
                where a.empresa_id=:e and a.estado='ACTIVO'
                """, "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public List<DashboardResumen.Distribucion> animalesPorPotrero(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return distribucion("""
                select coalesce(p.nombre,'Sin potrero') as nombre, count(*) as total
                from ganado.animales a
                left join campo.potreros p on p.id=a.potrero_actual_id
                where a.empresa_id=:e and a.estado='ACTIVO'
                """, "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public List<DashboardResumen.Distribucion> animalesPorLote(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return distribucion("""
                select coalesce(l.nombre,'Sin lote') as nombre, count(*) as total
                from ganado.animales a
                left join ganado.lotes_ganaderos l on l.id=a.lote_actual_id
                where a.empresa_id=:e and a.estado='ACTIVO'
                """, "a.propiedad_actual_id", empresa, todas, permitidas);
    }

    @Override
    public List<DashboardResumen.PesajeReciente> pesajesRecientes(UUID empresa, boolean todas, Set<UUID> permitidas, int limite) {
        if (!todas && permitidas.isEmpty()) return List.of();
        StringBuilder sql = new StringBuilder("""
                select p.id, p.animal_id, a.codigo as animal_codigo, a.nombre as animal_nombre, p.fecha, p.peso_kg
                from produccion.pesajes p
                join ganado.animales a on a.id=p.animal_id and a.empresa_id=p.empresa_id
                where p.empresa_id=:e and p.estado='ACTIVO'
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todas) {
            sql.append(" and p.propiedad_id in (:allowed)");
            params.put("allowed", permitidas);
        }
        sql.append(" order by p.fecha desc, p.created_at desc limit :n");
        params.put("n", limite);
        return jdbc.sql(sql.toString()).params(params).query((rs, rowNum) ->
                new DashboardResumen.PesajeReciente(
                        rs.getObject("id", UUID.class),
                        rs.getObject("animal_id", UUID.class),
                        rs.getString("animal_codigo"),
                        rs.getString("animal_nombre"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getBigDecimal("peso_kg"))).list();
    }

    private long count(String base, String propiedadColumna, UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count(base, propiedadColumna, empresa, todas, permitidas, Map.of());
    }

    private long count(String base, String propiedadColumna, UUID empresa, boolean todas, Set<UUID> permitidas,
                       Map<String, Object> extra) {
        if (!todas && permitidas.isEmpty()) return 0;
        Map<String, Object> params = new HashMap<>(extra);
        params.put("e", empresa);
        if (!todas) {
            base += " and " + propiedadColumna + " in (:allowed)";
            params.put("allowed", permitidas);
        }
        Long value = jdbc.sql(base).params(params).query(Long.class).single();
        return value == null ? 0 : value;
    }

    private Double scalar(String base, String propiedadColumna, UUID empresa, boolean todas, Set<UUID> permitidas) {
        if (!todas && permitidas.isEmpty()) return null;
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todas) {
            base += " and " + propiedadColumna + " in (:allowed)";
            params.put("allowed", permitidas);
        }
        return jdbc.sql(base).params(params).query(Double.class).optional().orElse(null);
    }

    private List<DashboardResumen.Distribucion> distribucion(String base, String propiedadColumna, UUID empresa,
                                                             boolean todas, Set<UUID> permitidas) {
        if (!todas && permitidas.isEmpty()) return List.of();
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todas) {
            base += " and " + propiedadColumna + " in (:allowed)";
            params.put("allowed", permitidas);
        }
        base += " group by 1 order by total desc, 1";
        return jdbc.sql(base).params(params).query((rs, rowNum) ->
                new DashboardResumen.Distribucion(rs.getString("nombre"), rs.getLong("total"))).list();
    }
}
