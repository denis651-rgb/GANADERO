package bo.com.ganadero.dashboard.infrastructure;

import bo.com.ganadero.dashboard.domain.DashboardRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
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
        return count("select count(*) from ganado.lotes_ganaderos where empresa_id=:e and estado='ABIERTO'",
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
    public long countPesajesUltimosDias(UUID empresa, int dias) {
        Long value = jdbc.sql("select count(*) from produccion.pesajes where empresa_id=:e and estado='ACTIVO' " +
                "and fecha >= current_date - :dias").param("e", empresa).param("dias", dias).query(Long.class).single();
        return value == null ? 0 : value;
    }

    @Override
    public long countMovimientosUltimosDias(UUID empresa, int dias) {
        Long value = jdbc.sql("select count(*) from ganado.movimientos where empresa_id=:e " +
                "and fecha_movimiento >= current_date - :dias").param("e", empresa).param("dias", dias).query(Long.class).single();
        return value == null ? 0 : value;
    }

    @Override
    public long countAnimalesSinPesaje(UUID empresa, boolean todas, Set<UUID> permitidas) {
        return count("select count(*) from produccion.v_animales_sin_pesaje where empresa_id=:e and dias_sin_pesaje > 30",
                "propiedad_actual_id", empresa, todas, permitidas);
    }

    private long count(String base, String propiedadColumna, UUID empresa, boolean todas, Set<UUID> permitidas) {
        if (!todas && permitidas.isEmpty()) return 0;
        Map<String, Object> params = new HashMap<>();
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
}
