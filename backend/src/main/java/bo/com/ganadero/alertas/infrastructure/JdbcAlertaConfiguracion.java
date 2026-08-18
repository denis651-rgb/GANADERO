package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.application.AlertaConfiguracion;
import bo.com.ganadero.alertas.application.AlertaConfiguracionPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
                               dias_diagnostico_post_servicio, dias_gestacion_estimada
                        from core.configuraciones_empresa
                        where empresa_id = :empresaId
                        """)
                .param("empresaId", empresaId)
                .query((rs, rowNum) -> new AlertaConfiguracion(
                        rs.getInt("dias_alerta_preparto"),
                        rs.getInt("dias_alerta_destete"),
                        rs.getInt("dias_diagnostico_post_servicio"),
                        rs.getInt("dias_gestacion_estimada")))
                .optional()
                .orElseGet(AlertaConfiguracion::valoresPredeterminados);
    }
}
