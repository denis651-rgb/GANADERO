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
 * Migración de solo-lectura (no cambia datos): antes de habilitar "Mover lote" sobre el modelo
 * de ubicación existente, detecta y reporta inconsistencias entre propiedad/potrero/lote que
 * pudieran existir en la base ya en uso. No corrige nada de forma silenciosa (docs sección 18):
 * si un dato es ambiguo, se deja tal cual y se informa por log para revisión manual.
 */
public class V25__ReportarInconsistenciasUbicacion extends BaseJavaMigration {
    private static final Logger LOG = LoggerFactory.getLogger(V25__ReportarInconsistenciasUbicacion.class);

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        List<String> potreroAjeno = animalesConPotreroDeOtraPropiedad(conn);
        List<String> loteAjeno = animalesConLoteDeOtraPropiedad(conn);
        List<String> lotesDivididos = lotesConMiembrosEnVariasPropiedades(conn);

        if (potreroAjeno.isEmpty() && loteAjeno.isEmpty() && lotesDivididos.isEmpty()) {
            LOG.info("Reporte de inconsistencias de ubicación: no se encontraron inconsistencias.");
            return;
        }
        LOG.warn("Reporte de inconsistencias de ubicación (no corregidas automáticamente): "
                + "{} animal(es) con potrero de otra propiedad {}; "
                + "{} animal(es) con lote de otra propiedad {}; "
                + "{} lote(s) con miembros activos repartidos en más de una propiedad {}.",
                potreroAjeno.size(), potreroAjeno, loteAjeno.size(), loteAjeno,
                lotesDivididos.size(), lotesDivididos);
    }

    private List<String> animalesConPotreroDeOtraPropiedad(Connection conn) throws Exception {
        String sql = """
                select a.codigo from animal a
                join potrero p on p.id = a.potrero_actual_id
                where a.potrero_actual_id is not null and p.propiedad_id <> a.propiedad_actual_id
                """;
        return codigos(conn, sql);
    }

    private List<String> animalesConLoteDeOtraPropiedad(Connection conn) throws Exception {
        String sql = """
                select a.codigo from animal a
                join lote_ganadero l on l.id = a.lote_actual_id
                where a.lote_actual_id is not null and l.propiedad_id <> a.propiedad_actual_id
                """;
        return codigos(conn, sql);
    }

    private List<String> lotesConMiembrosEnVariasPropiedades(Connection conn) throws Exception {
        String sql = """
                select l.codigo from lote_ganadero l
                where (
                    select count(distinct a.propiedad_actual_id)
                    from membresia_lote m join animal a on a.id = m.animal_id
                    where m.lote_id = l.id and m.fecha_salida is null
                ) > 1
                """;
        return codigos(conn, sql);
    }

    private List<String> codigos(Connection conn, String sql) throws Exception {
        List<String> resultado = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultado.add(rs.getString(1));
        }
        return resultado;
    }
}
