package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.sanidad.domain.OcurrenciaCalendarioSanitario;
import bo.com.ganadero.sanidad.domain.OcurrenciaCalendarioSanitarioRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcOcurrenciaCalendarioSanitarioRepository implements OcurrenciaCalendarioSanitarioRepository {
    private final JdbcClient jdbc;

    public JdbcOcurrenciaCalendarioSanitarioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID crearOUsar(OcurrenciaCalendarioSanitario o) {
        jdbc.sql("""
                insert into ocurrencia_calendario_sanitario
                    (id, plan_item_id, fecha_prevista, propiedad_id, potrero_id, lote_ganadero_id, ocurrencia_clave)
                values (:id, :item, :fecha, :prop, :potrero, :lote, :clave)
                on conflict(ocurrencia_clave) do nothing
                """)
                .param("id", o.id().toString())
                .param("item", o.planItemId().toString())
                .param("fecha", o.fechaPrevista().toString())
                .param("prop", o.propiedadId().toString())
                .param("potrero", o.potreroId().toString())
                .param("lote", o.loteGanaderoId() == null ? null : o.loteGanaderoId().toString())
                .param("clave", o.ocurrenciaClave())
                .update();
        return UUID.fromString(jdbc.sql("select id from ocurrencia_calendario_sanitario where ocurrencia_clave=:clave")
                .param("clave", o.ocurrenciaClave())
                .query(String.class).single());
    }
}
