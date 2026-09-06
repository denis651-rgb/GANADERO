package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Infiere `modalidad`/`modalidad_config` para cada actividad sanitaria migrada por V27, con
 * reglas verificables únicamente (nunca se inventa una modalidad): si tiene `frecuencia_dias`
 * se asume PERIODICA (referencia ULTIMA_APLICACION, que es el comportamiento que ya tenía el
 * código antes de esta migración); si no, pero tiene `edad_min_dias`, se asume POR_EDAD (una
 * sola vez en la vida, que es como se usaba hasta ahora para vacunas por edad); en cualquier
 * otro caso queda MANUAL y se marca `requiere_revision=1` (además de las filas que V27 ya marcó
 * por tener `tipo_actividad_legacy='CONTROL'`, ambiguo). Migración Java (no .sql) porque la
 * decisión if/else no es practica de expresar en SQL puro y porque se necesita el mismo tipo de
 * informe legible que ya generan V22/V25.
 */
public class V28__MigrarModalidadActividadSanitaria extends BaseJavaMigration {
    private static final Logger LOG = LoggerFactory.getLogger(V28__MigrarModalidadActividadSanitaria.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        List<Item> items = pendientes(conn);
        int periodica = 0, porEdad = 0, manualRequiereRevision = 0, manualYaMarcada = 0;

        for (Item item : items) {
            String modalidad;
            String config;
            boolean requiereRevision = item.requiereRevisionPrevia;

            if (item.frecuenciaDias != null && item.frecuenciaDias > 0) {
                modalidad = "PERIODICA";
                config = """
                        {"frecuenciaValor":%d,"frecuenciaUnidad":"DIAS","referenciaCalculo":"ULTIMA_APLICACION",\
                        "toleranciaAnticipadaDias":%d,"toleranciaPosteriorDias":0}\
                        """.formatted(item.frecuenciaDias, item.diasAlerta);
                periodica++;
            } else if (item.edadMinDias != null) {
                int ventanaPosterior = item.edadMaxDias != null && item.edadMaxDias > item.edadMinDias
                        ? item.edadMaxDias - item.edadMinDias : 0;
                modalidad = "POR_EDAD";
                config = """
                        {"edadObjetivoValor":%d,"edadUnidad":"DIAS","ventanaAnticipadaDias":%d,\
                        "ventanaPosteriorDias":%d,"politicaEdadEstimada":"PERMITIR",\
                        "politicaEdadDesconocida":"%s","unaVezEnLaVida":true}\
                        """.formatted(item.edadMinDias, item.diasAlerta, ventanaPosterior,
                        item.permiteEdadDesconocida ? "INCLUIR_MANUAL" : "EXCLUIR");
                porEdad++;
            } else {
                modalidad = "MANUAL";
                config = "{}";
                if (!requiereRevision) {
                    requiereRevision = true;
                    manualRequiereRevision++;
                } else {
                    manualYaMarcada++;
                }
            }

            actualizar(conn, item.id, modalidad, config, requiereRevision);
        }

        LOG.info("Migración de modalidad de actividades sanitarias: {} a PERIODICA, {} a POR_EDAD, "
                + "{} a MANUAL marcadas requiere_revision (sin contar {} ya marcadas por V27 por ser "
                + "tipo_actividad_legacy='CONTROL').",
                periodica, porEdad, manualRequiereRevision, manualYaMarcada);
    }

    private List<Item> pendientes(Connection conn) throws Exception {
        List<Item> resultado = new ArrayList<>();
        String sql = "select id, frecuencia_dias, edad_min_dias, edad_max_dias, dias_alerta, "
                + "permite_edad_desconocida, requiere_revision from plan_sanitario_item";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new Item(rs.getString("id"),
                        rs.getObject("frecuencia_dias") == null ? null : rs.getInt("frecuencia_dias"),
                        rs.getObject("edad_min_dias") == null ? null : rs.getInt("edad_min_dias"),
                        rs.getObject("edad_max_dias") == null ? null : rs.getInt("edad_max_dias"),
                        rs.getInt("dias_alerta"), rs.getInt("permite_edad_desconocida") != 0,
                        rs.getInt("requiere_revision") != 0));
            }
        }
        return resultado;
    }

    private void actualizar(Connection conn, String id, String modalidad, String config, boolean requiereRevision)
            throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "update plan_sanitario_item set modalidad=?, modalidad_config=?, requiere_revision=? where id=?")) {
            ps.setString(1, modalidad);
            ps.setString(2, config);
            ps.setInt(3, requiereRevision ? 1 : 0);
            ps.setString(4, id);
            ps.executeUpdate();
        }
    }

    private record Item(String id, Integer frecuenciaDias, Integer edadMinDias, Integer edadMaxDias,
                        int diasAlerta, boolean permiteEdadDesconocida, boolean requiereRevisionPrevia) {
    }
}
