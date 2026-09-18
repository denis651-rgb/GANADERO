package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.AlertaConfiguracion;
import bo.com.ganadero.alertas.application.AlertaConfiguracionPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Repository
public class JdbcAlertaConfiguracion implements AlertaConfiguracionPort {
    private final JdbcClient jdbc;

    public JdbcAlertaConfiguracion(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public AlertaConfiguracion obtener(UUID empresaId) {
        return jdbc.sql("""
                        select dias_alerta_preparto, dias_alerta_destete,
                               dias_diagnostico_post_servicio, dias_gestacion_estimada, hora_avisos
                        from configuracion
                        """)
                .query((rs, rowNum) -> new AlertaConfiguracion(
                        rs.getInt("dias_alerta_preparto"),
                        rs.getInt("dias_alerta_destete"),
                        rs.getInt("dias_diagnostico_post_servicio"),
                        rs.getInt("dias_gestacion_estimada"),
                        hora(rs.getString("hora_avisos"))))
                .optional()
                .orElseGet(AlertaConfiguracion::valoresPredeterminados);
    }

    /** Un valor ilegible nunca debe dejar sin avisos: cae en la hora predeterminada. */
    private static LocalTime hora(String valor) {
        try {
            return valor == null ? null : LocalTime.parse(valor);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
