package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProcesarPesajesAtrasadosService {
    private final JdbcClient jdbc;
    private final MotorAlertas alertas;

    public ProcesarPesajesAtrasadosService(JdbcClient jdbc, MotorAlertas alertas) {
        this.jdbc = jdbc;
        this.alertas = alertas;
    }

    @Transactional
    public int procesar() {
        List<PesajeAtrasado> atrasados = jdbc.sql("""
                        select a.id, a.codigo, a.nombre,
                               (select max(p.fecha) from pesaje p where p.animal_id=a.id and p.estado='ACTIVO') as ultimo_pesaje,
                               cast(julianday('now') - julianday(coalesce(
                                   (select max(p.fecha) from pesaje p where p.animal_id=a.id and p.estado='ACTIVO'), a.created_at)) as integer) as dias_sin_pesaje
                        from animal a, propiedad c
                        where a.estado='ACTIVO' and c.dias_sin_pesaje > 0
                        having dias_sin_pesaje > c.dias_sin_pesaje
                        order by dias_sin_pesaje desc
                        limit 1000
                        """)
                .query((rs, rowNum) -> new PesajeAtrasado(
                        Rows.uuid(rs, "id"), null,
                        rs.getString("codigo"), rs.getString("nombre"),
                        rs.getString("ultimo_pesaje") == null ? null : LocalDate.parse(rs.getString("ultimo_pesaje")),
                        rs.getLong("dias_sin_pesaje")))
                .list();

        Instant ahora = Instant.now();
        for (PesajeAtrasado atrasado : atrasados) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("animalCodigo", atrasado.codigo());
            if (atrasado.nombre() != null && !atrasado.nombre().isBlank()) {
                metadata.put("animalNombre", atrasado.nombre());
            }
            metadata.put("diasSinPesaje", atrasado.diasSinPesaje());
            metadata.put("eventoReferencia", atrasado.ultimoPesaje() == null
                    ? "SIN_PESAJE" : atrasado.ultimoPesaje().toString());
            if (atrasado.ultimoPesaje() != null) {
                metadata.put("ultimoPesaje", atrasado.ultimoPesaje().toString());
            }
            alertas.crearInmediata(new ProgramarAlertaCommand(atrasado.empresaId(), atrasado.animalId(),
                    TipoAlerta.PESAJE_ATRASADO, ahora, "ANIMAL", atrasado.animalId(), metadata));
        }
        return atrasados.size();
    }

    private record PesajeAtrasado(UUID animalId, UUID empresaId, String codigo,
                                  String nombre, LocalDate ultimoPesaje, long diasSinPesaje) {}
}
