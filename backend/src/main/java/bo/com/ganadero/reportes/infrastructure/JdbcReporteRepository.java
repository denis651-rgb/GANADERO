package bo.com.ganadero.reportes.infrastructure;

import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.reportes.domain.ReporteAnimalMuerto;
import bo.com.ganadero.reportes.domain.ReporteAnimalNacido;
import bo.com.ganadero.reportes.domain.ReporteRepository;
import bo.com.ganadero.reportes.domain.ReporteVenta;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.ventas.domain.ModalidadVenta;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Consultas de solo lectura para el reporte de movimientos del hato. La muerte de un animal no
 * tiene tabla propia: es una fila más en {@code evento_animal} (tipo ESTADO_CAMBIADO) publicada
 * por el módulo timeline, que solo graba el detalle como JSON en la columna {@code metadata}
 * (las columnas planas estado_anterior/estado_nuevo/motivo de esa tabla no se pueblan por ese
 * camino) — por eso la consulta de muertes usa {@code json_extract}, mismo recurso que ya usa
 * {@code alertas.infrastructure.JdbcRecordatorioRepository} sobre esa misma columna.
 */
@Repository
public class JdbcReporteRepository implements ReporteRepository {
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");

    private final JdbcClient jdbc;

    public JdbcReporteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ReporteAnimalNacido> nacidos(LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                select a.id, a.codigo, a.nombre, a.sexo, a.fecha_nacimiento, a.fecha_nacimiento_estimada,
                       r.nombre as raza, c.nombre as categoria, a.peso_nacimiento_kg,
                       pr.nombre as propiedad, po.nombre as potrero,
                       (select coalesce(ma.nombre, ma.codigo, pm.nombre_externo)
                          from parentesco pm left join animal ma on ma.id = pm.animal_padre_id
                         where pm.animal_id = a.id and pm.tipo_parentesco = 'MADRE' limit 1) as madre,
                       (select coalesce(pdre.nombre, pdre.codigo, pp.nombre_externo)
                          from parentesco pp left join animal pdre on pdre.id = pp.animal_padre_id
                         where pp.animal_id = a.id and pp.tipo_parentesco = 'PADRE' limit 1) as padre
                from animal a
                join raza r on r.id = a.raza_principal_id
                join categoria_animal c on c.id = a.categoria_actual_id
                left join propiedad pr on pr.id = a.propiedad_actual_id
                left join potrero po on po.id = a.potrero_actual_id
                where a.origen = 'NACIDO' and a.fecha_nacimiento >= :desde and a.fecha_nacimiento <= :hasta
                order by a.fecha_nacimiento desc, a.codigo
                """)
                .params(Map.of("desde", desde.toString(), "hasta", hasta.toString()))
                .query(this::mapNacido)
                .list();
    }

    @Override
    public List<ReporteAnimalMuerto> muertos(LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                select a.id, a.codigo, a.nombre, a.sexo, r.nombre as raza, c.nombre as categoria,
                       e.fecha_evento as fecha_muerte, json_extract(e.metadata, '$.motivo') as motivo,
                       pr.nombre as propiedad, po.nombre as potrero
                from evento_animal e
                join animal a on a.id = e.animal_id
                join raza r on r.id = a.raza_principal_id
                join categoria_animal c on c.id = a.categoria_actual_id
                left join propiedad pr on pr.id = a.propiedad_actual_id
                left join potrero po on po.id = a.potrero_actual_id
                where e.tipo = 'ESTADO_CAMBIADO' and json_extract(e.metadata, '$.estadoNuevo') = 'MUERTO'
                  and e.fecha_evento >= :desde and e.fecha_evento < :hastaExclusivo
                order by e.fecha_evento desc
                """)
                .params(Map.of("desde", desde.toString(), "hastaExclusivo", hasta.plusDays(1).toString()))
                .query(this::mapMuerto)
                .list();
    }

    @Override
    public List<ReporteVenta> ventas(LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                select v.id as venta_id, v.animal_id, a.codigo, a.nombre, r.nombre as raza,
                       v.fecha_venta, v.comprador, v.telefono_comprador, v.precio, v.moneda,
                       v.peso_venta_kg, v.modalidad, v.precio_unitario
                from venta v
                join animal a on a.id = v.animal_id
                join raza r on r.id = a.raza_principal_id
                where v.fecha_venta >= :desde and v.fecha_venta <= :hasta
                order by v.fecha_venta desc
                """)
                .params(Map.of("desde", desde.toString(), "hasta", hasta.toString()))
                .query(this::mapVenta)
                .list();
    }

    private ReporteAnimalNacido mapNacido(ResultSet r, int row) throws SQLException {
        return new ReporteAnimalNacido(Rows.uuid(r, "id"), r.getString("codigo"), r.getString("nombre"),
                SexoAnimal.valueOf(r.getString("sexo")), Rows.localDate(r, "fecha_nacimiento"),
                r.getBoolean("fecha_nacimiento_estimada"), r.getString("raza"), r.getString("categoria"),
                r.getBigDecimal("peso_nacimiento_kg"), r.getString("propiedad"), r.getString("potrero"),
                r.getString("madre"), r.getString("padre"));
    }

    private ReporteAnimalMuerto mapMuerto(ResultSet r, int row) throws SQLException {
        LocalDate fechaMuerte = Rows.instant(r, "fecha_muerte").atZone(BOLIVIA).toLocalDate();
        return new ReporteAnimalMuerto(Rows.uuid(r, "id"), r.getString("codigo"), r.getString("nombre"),
                SexoAnimal.valueOf(r.getString("sexo")), r.getString("raza"), r.getString("categoria"),
                fechaMuerte, r.getString("motivo"), r.getString("propiedad"), r.getString("potrero"));
    }

    private ReporteVenta mapVenta(ResultSet r, int row) throws SQLException {
        return new ReporteVenta(Rows.uuid(r, "venta_id"), Rows.uuid(r, "animal_id"), r.getString("codigo"),
                r.getString("nombre"), r.getString("raza"), Rows.localDate(r, "fecha_venta"),
                r.getString("comprador"), r.getString("telefono_comprador"), r.getBigDecimal("precio"),
                r.getString("moneda"), r.getBigDecimal("peso_venta_kg"),
                ModalidadVenta.valueOf(r.getString("modalidad")), r.getBigDecimal("precio_unitario"));
    }
}
