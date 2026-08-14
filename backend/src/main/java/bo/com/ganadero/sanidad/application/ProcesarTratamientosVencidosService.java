package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
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

    @Scheduled(cron = "${ganadero.sanidad.cron-tratamientos-vencidos:0 */15 * * * *}")
    @Transactional
    public int procesar() {
        List<Vencida> vencidas = jdbc.sql("""
                        select a.id, a.empresa_id, t.animal_id, a.fecha_programada,
                               an.codigo, an.nombre
                        from sanidad.aplicaciones_tratamiento a
                        join sanidad.tratamiento_detalles d on d.id = a.tratamiento_detalle_id
                        join sanidad.tratamientos t on t.id = d.tratamiento_id
                        join ganado.animales an on an.id = t.animal_id and an.empresa_id = a.empresa_id
                        where a.estado = 'PENDIENTE'
                          and a.fecha_programada < now()
                          and t.estado = 'ACTIVO'
                        order by a.fecha_programada
                        for update of a skip locked
                        limit 100
                        """)
                .query((rs, rowNum) -> new Vencida(
                        rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                        rs.getObject("animal_id", UUID.class), odt(rs, "fecha_programada"),
                        rs.getString("codigo"), rs.getString("nombre")))
                .list();
        for (Vencida vencida : vencidas) {
            int actualizadas = jdbc.sql("""
                            update sanidad.aplicaciones_tratamiento
                            set estado = 'ATRASADA', updated_at = now(), version = version + 1
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

    private static Instant odt(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime value = rs.getObject(columna, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private record Vencida(UUID id, UUID empresa, UUID animal, Instant fecha,
                           String codigo, String nombre) {}
}
