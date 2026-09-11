package bo.com.ganadero.ventas.infrastructure;

import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.ventas.domain.ModalidadVenta;
import bo.com.ganadero.ventas.domain.Venta;
import bo.com.ganadero.ventas.domain.VentaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcVentaRepository implements VentaRepository {
    private final JdbcClient jdbc;

    public JdbcVentaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Venta create(Venta v) {
        jdbc.sql("""
                insert into venta(id,animal_id,movimiento_id,fecha_venta,comprador,precio,moneda,
                    peso_venta_kg,observaciones,created_by,telefono_comprador,modalidad,precio_unitario,grupo_venta_id)
                values(:id,:animal,:movimiento,:fecha,:comprador,:precio,:moneda,:peso,:obs,:actor,
                    :telefono,:modalidad,:precioUnitario,:grupo)
                """)
                .param("id", v.id().toString())
                .param("animal", v.animalId().toString())
                .param("movimiento", v.movimientoId() == null ? null : v.movimientoId().toString())
                .param("fecha", v.fechaVenta().toString())
                .param("comprador", v.comprador())
                .param("precio", v.precio())
                .param("moneda", v.moneda())
                .param("peso", v.pesoVentaKg())
                .param("obs", v.observaciones())
                .param("actor", v.createdBy() == null ? null : v.createdBy().toString())
                .param("telefono", v.telefonoComprador())
                .param("modalidad", v.modalidad().name())
                .param("precioUnitario", v.precioUnitario())
                .param("grupo", v.grupoVentaId() == null ? null : v.grupoVentaId().toString())
                .update();
        return findById(v.id()).orElseThrow();
    }

    @Override
    public Optional<Venta> findById(UUID id) {
        return jdbc.sql("select * from venta where id=:id").param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public List<Venta> findAll(UUID animalId, LocalDate desde, LocalDate hasta) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (animalId != null) {
            where.append(" and animal_id=:animal");
            params.put("animal", animalId.toString());
        }
        if (desde != null) {
            where.append(" and fecha_venta>=:desde");
            params.put("desde", desde.toString());
        }
        if (hasta != null) {
            where.append(" and fecha_venta<=:hasta");
            params.put("hasta", hasta.toString());
        }
        return jdbc.sql("select * from venta" + where + " order by fecha_venta desc, created_at desc")
                .params(params).query(this::map).list();
    }

    private Venta map(ResultSet r, int row) throws SQLException {
        return new Venta(Rows.uuid(r, "id"), Rows.uuid(r, "animal_id"), Rows.uuid(r, "movimiento_id"),
                LocalDate.parse(r.getString("fecha_venta")), r.getString("comprador"),
                r.getBigDecimal("precio"), r.getString("moneda"), r.getBigDecimal("peso_venta_kg"),
                r.getString("observaciones"), Rows.uuid(r, "created_by"), Rows.instant(r, "created_at"),
                r.getLong("version"), r.getString("telefono_comprador"),
                ModalidadVenta.valueOf(r.getString("modalidad")), r.getBigDecimal("precio_unitario"),
                Rows.uuid(r, "grupo_venta_id"));
    }
}
