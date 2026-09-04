package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.movimientos.application.EstadoSanitarioIngresoPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Implementa el puerto que pide movimientos (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md,
 * secciones 3 y 7): antes de permitir RETORNO_CUARENTENA, movimientos necesita saber si
 * el animal tiene una prueba diagnostica registrada desde su ingreso, sin importar a las
 * tablas internas de sanidad (regla del modulith, docs/backend/MODULOS.md). Mismo estilo
 * de JdbcClient + SQL directo que ya usa ProcesarAlertasVacunacionService.
 */
@Component
public class EstadoSanitarioIngresoAdapter implements EstadoSanitarioIngresoPort {
    private final JdbcClient jdbc;

    public EstadoSanitarioIngresoAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tienePruebaDiagnosticaDesde(UUID empresaId, UUID animalId, LocalDate desde) {
        return jdbc.sql("""
                        select exists(
                            select 1 from aplicacion_sanitaria a
                            join plan_sanitario_item i on i.id = a.plan_item_id
                            where a.animal_id = :animal
                              and a.estado = 'APLICADA'
                              and a.fecha_aplicacion >= :desde
                              and i.tipo_actividad = 'PRUEBA_DIAGNOSTICA'
                        )
                        """)
                .param("animal", animalId.toString())
                .param("desde", desde.toString())
                .query(Boolean.class).single();
    }
}
