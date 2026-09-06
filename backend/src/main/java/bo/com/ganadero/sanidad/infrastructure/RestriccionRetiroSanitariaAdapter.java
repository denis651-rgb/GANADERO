package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.ventas.application.RestriccionRetiroPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa el puerto que {@code VentaService} usa para comprobar restricciones de retiro
 * (sección 24), consultando tanto las aplicaciones de jornada como las de tratamiento libre —
 * ambas calculadas desde la ejecución real, nunca desde inventario.
 */
@Component
class RestriccionRetiroSanitariaAdapter implements RestriccionRetiroPort {
    private final JdbcClient jdbc;

    RestriccionRetiroSanitariaAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RestriccionRetiroVigente> vigente(UUID empresaId, UUID animalId, LocalDate fechaVenta) {
        String fecha = fechaVenta.toString();
        Optional<RestriccionRetiroVigente> deJornada = jdbc.sql("""
                select retiro_carne_hasta, retiro_leche_hasta from aplicacion_sanitaria
                where animal_id=:a and estado='APLICADO'
                    and (retiro_carne_hasta>=:f or retiro_leche_hasta>=:f)
                order by coalesce(retiro_carne_hasta,retiro_leche_hasta) desc limit 1
                """).param("a", animalId.toString()).param("f", fecha)
                .query((rs, n) -> mapRow(rs.getString("retiro_carne_hasta"), rs.getString("retiro_leche_hasta"), fechaVenta))
                .optional();
        if (deJornada.isPresent()) return deJornada;

        return jdbc.sql("""
                select at.retiro_carne_hasta, at.retiro_leche_hasta from aplicacion_tratamiento at
                join tratamiento_detalle d on d.id = at.tratamiento_detalle_id
                join tratamiento t on t.id = d.tratamiento_id
                where t.animal_id=:a and at.estado='APLICADA'
                    and (at.retiro_carne_hasta>=:f or at.retiro_leche_hasta>=:f)
                order by coalesce(at.retiro_carne_hasta,at.retiro_leche_hasta) desc limit 1
                """).param("a", animalId.toString()).param("f", fecha)
                .query((rs, n) -> mapRow(rs.getString("retiro_carne_hasta"), rs.getString("retiro_leche_hasta"), fechaVenta))
                .optional();
    }

    private RestriccionRetiroVigente mapRow(String carne, String leche, LocalDate fechaVenta) {
        LocalDate carneHasta = carne == null ? null : LocalDate.parse(carne);
        LocalDate lecheHasta = leche == null ? null : LocalDate.parse(leche);
        if (carneHasta != null && !carneHasta.isBefore(fechaVenta)) return new RestriccionRetiroVigente("CARNE", carneHasta);
        return new RestriccionRetiroVigente("LECHE", lecheHasta);
    }
}
