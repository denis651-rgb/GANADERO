package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProcesarTratamientosVencidosService {
    private final JdbcClient jdbc;
    private final MotorAlertas alertas;

    public ProcesarTratamientosVencidosService(JdbcClient jdbc, MotorAlertas alertas) {
        this.jdbc = jdbc;
        this.alertas = alertas;
    }

    @Transactional
    public int procesar() {
        List<Vencida> vencidas = jdbc.sql("""
                        select a.id, t.animal_id, a.fecha_programada,
                               an.codigo, an.nombre
                        from aplicacion_tratamiento a
                        join tratamiento_detalle d on d.id = a.tratamiento_detalle_id
                        join tratamiento t on t.id = d.tratamiento_id
                        join animal an on an.id = t.animal_id
                        where a.estado = 'PENDIENTE'
                          and a.fecha_programada < strftime('%Y-%m-%dT%H:%M:%fZ','now')
                          and t.estado = 'ACTIVO'
                        order by a.fecha_programada
                        limit 100
                        """)
                .query((rs, rowNum) -> new Vencida(
                        Rows.uuid(rs, "id"), null,
                        Rows.uuid(rs, "animal_id"), Rows.instant(rs, "fecha_programada"),
                        rs.getString("codigo"), rs.getString("nombre")))
                .list();
        for (Vencida vencida : vencidas) {
            int actualizadas = jdbc.sql("""
                            update aplicacion_tratamiento
                            set estado = 'ATRASADA', updated_at = strftime('%Y-%m-%dT%H:%M:%fZ','now'), version = version + 1
                            where id = :id and estado = 'PENDIENTE'
                            """)
                    .param("id", vencida.id()).update();
            if (actualizadas == 0) continue;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fechaProgramada", vencida.fecha().toString());
            metadata.put("animalCodigo", vencida.codigo());
            if (vencida.nombre() != null && !vencida.nombre().isBlank()) {
                metadata.put("animalNombre", vencida.nombre());
            }
            alertas.crearInmediata(new ProgramarAlertaCommand(vencida.empresa(), vencida.animal(),
                    TipoAlerta.TRATAMIENTO_ATRASADO, Instant.now(), "APLICACION_TRATAMIENTO",
                    vencida.id(), metadata));
        }
        return vencidas.size();
    }

    private record Vencida(UUID id, UUID empresa, UUID animal, Instant fecha,
                           String codigo, String nombre) {}
}
