package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProcesarAlertasVacunacionService {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");
    private static final Set<TipoAlerta> TIPOS_VACUNACION =
            Set.of(TipoAlerta.VACUNA_PROXIMA, TipoAlerta.VACUNA_VENCIDA);

    private final JdbcClient jdbc;
    private final MotorAlertas alertas;

    public ProcesarAlertasVacunacionService(JdbcClient jdbc, MotorAlertas alertas) {
        this.jdbc = jdbc;
        this.alertas = alertas;
    }

    @Scheduled(cron = "${ganadero.sanidad.cron-alertas-vacunacion:0 5 0 * * *}")
    @Transactional
    public int procesar() {
        return procesar(LocalDate.now(ZONA_NEGOCIO));
    }

    int procesar(LocalDate hoy) {
        List<VacunacionPendiente> pendientes = jdbc.sql("""
                        select a.id, a.empresa_id, a.animal_id, a.proxima_aplicacion,
                               i.dias_alerta, an.codigo, an.nombre
                        from sanidad.aplicaciones_sanitarias a
                        join sanidad.plan_sanitario_items i on i.id = a.plan_item_id
                        join ganado.animales an on an.id = a.animal_id and an.empresa_id = a.empresa_id
                        where a.estado = 'APLICADA'
                          and i.tipo_actividad = 'VACUNACION'
                          and a.proxima_aplicacion is not null
                          and a.proxima_aplicacion <= :hoy + i.dias_alerta
                          and not exists (
                              select 1 from sanidad.aplicaciones_sanitarias nueva
                              where nueva.empresa_id = a.empresa_id
                                and nueva.animal_id = a.animal_id
                                and nueva.plan_item_id = a.plan_item_id
                                and nueva.estado = 'APLICADA'
                                and nueva.fecha_aplicacion > a.fecha_aplicacion
                          )
                        order by a.proxima_aplicacion
                        limit 500
                        """)
                .param("hoy", hoy)
                .query((rs, rowNum) -> new VacunacionPendiente(
                        rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                        rs.getObject("animal_id", UUID.class),
                        rs.getObject("proxima_aplicacion", LocalDate.class),
                        rs.getInt("dias_alerta"), rs.getString("codigo"), rs.getString("nombre")))
                .list();

        Instant ahora = Instant.now();
        for (VacunacionPendiente pendiente : pendientes) {
            int diasRestantes = Math.toIntExact(ChronoUnit.DAYS.between(hoy, pendiente.proximaAplicacion()));
            TipoAlerta tipo = diasRestantes <= 0 ? TipoAlerta.VACUNA_VENCIDA : TipoAlerta.VACUNA_PROXIMA;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("animalCodigo", pendiente.codigo());
            if (pendiente.nombre() != null && !pendiente.nombre().isBlank()) {
                metadata.put("animalNombre", pendiente.nombre());
            }
            metadata.put("diasRestantes", diasRestantes);
            metadata.put("fechaProximaAplicacion", pendiente.proximaAplicacion().toString());
            metadata.put("eventoReferencia", pendiente.proximaAplicacion().toString());
            Instant vencimiento = pendiente.proximaAplicacion().atStartOfDay(ZONA_NEGOCIO).toInstant();
            alertas.evolucionar(new ProgramarAlertaCommand(pendiente.empresaId(), pendiente.animalId(), tipo,
                    ahora, vencimiento, "APLICACION_SANITARIA", pendiente.id(), metadata), TIPOS_VACUNACION);
        }
        return pendientes.size();
    }

    private record VacunacionPendiente(UUID id, UUID empresaId, UUID animalId,
                                       LocalDate proximaAplicacion, int diasAlerta,
                                       String codigo, String nombre) {}
}
