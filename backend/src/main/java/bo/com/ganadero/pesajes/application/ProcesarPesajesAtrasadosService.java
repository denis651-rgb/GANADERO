package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
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
                        select v.id, v.empresa_id, v.codigo, v.nombre,
                               v.ultimo_pesaje, v.dias_sin_pesaje
                        from produccion.v_animales_sin_pesaje v
                        join core.configuraciones_empresa c on c.empresa_id = v.empresa_id
                        where c.dias_sin_pesaje > 0
                          and v.dias_sin_pesaje > c.dias_sin_pesaje
                        order by v.dias_sin_pesaje desc
                        limit 1000
                        """)
                .query((rs, rowNum) -> new PesajeAtrasado(
                        rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                        rs.getString("codigo"), rs.getString("nombre"),
                        rs.getObject("ultimo_pesaje", LocalDate.class), rs.getLong("dias_sin_pesaje")))
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
