package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.movimientolote.application.RestriccionSanitariaPort;
import bo.com.ganadero.movimientolote.domain.RestriccionSanitaria;
import bo.com.ganadero.movimientolote.domain.SeveridadRestriccion;
import bo.com.ganadero.sanidad.domain.RestriccionMovimiento;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementa el puerto que "Mover lote" (movimientolote) usa para conocer restricciones
 * sanitarias de traslado, sin que ese módulo consulte directamente las tablas de sanidad
 * (docs/backend/MODULOS.md). Mismo estilo que EstadoSanitarioIngresoAdapter.
 *
 * <p>El efecto sobre el movimiento (BLOQUEANTE/ADVERTENCIA/INFORMATIVA) es un campo explícito
 * de {@code caso_clinico}/{@code tratamiento} ({@link RestriccionMovimiento}), fijado por el
 * veterinario al registrar el caso o tratamiento (con un valor por defecto calculado por
 * {@code ClinicaService} cuando no lo especifica). Este adaptador solo traduce ese dato del
 * dominio sanitario al vocabulario de severidad que espera "Mover lote"; no reinterpreta ni
 * infiere nada a partir de la severidad clínica.</p>
 */
@Component
public class RestriccionSanitariaTrasladoAdapter implements RestriccionSanitariaPort {
    private final JdbcClient jdbc;

    public RestriccionSanitariaTrasladoAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RestriccionSanitaria> evaluar(UUID empresaId, UUID animalId) {
        List<RestriccionSanitaria> restricciones = new ArrayList<>();
        String animal = animalId.toString();

        jdbc.sql("""
                select severidad, restriccion_movimiento from caso_clinico where animal_id=:a
                    and estado in ('ABIERTO','EN_OBSERVACION','EN_TRATAMIENTO')
                """).param("a", animal).query((rs, n) -> new String[]{rs.getString("severidad"), rs.getString("restriccion_movimiento")})
                .list().forEach(fila -> {
                    String severidad = fila[0];
                    RestriccionMovimiento restriccion = fila[1] == null ? RestriccionMovimiento.INFORMATIVA
                            : RestriccionMovimiento.valueOf(fila[1]);
                    restricciones.add(new RestriccionSanitaria("CASO_CLINICO_" + severidad, traducir(restriccion),
                            mensajeCaso(restriccion, severidad)));
                });

        jdbc.sql("select restriccion_movimiento from tratamiento where animal_id=:a and estado='ACTIVO'")
                .param("a", animal).query(String.class).list().forEach(valor -> {
                    RestriccionMovimiento restriccion = valor == null ? RestriccionMovimiento.ADVERTENCIA
                            : RestriccionMovimiento.valueOf(valor);
                    restricciones.add(new RestriccionSanitaria("TRATAMIENTO_ACTIVO", traducir(restriccion),
                            mensajeTratamiento(restriccion)));
                });

        return restricciones;
    }

    private SeveridadRestriccion traducir(RestriccionMovimiento restriccion) {
        return switch (restriccion) {
            case BLOQUEANTE -> SeveridadRestriccion.BLOQUEANTE;
            case ADVERTENCIA -> SeveridadRestriccion.ADVERTENCIA;
            case INFORMATIVA -> SeveridadRestriccion.INFORMATIVA;
        };
    }

    private String mensajeCaso(RestriccionMovimiento restriccion, String severidad) {
        return switch (restriccion) {
            case BLOQUEANTE -> "Tiene un caso clínico abierto (" + severidad + ") que requiere aislamiento.";
            case ADVERTENCIA -> "Tiene un caso clínico abierto (" + severidad + ").";
            case INFORMATIVA -> "Tiene un caso clínico abierto de severidad " + severidad + ".";
        };
    }

    private String mensajeTratamiento(RestriccionMovimiento restriccion) {
        return switch (restriccion) {
            case BLOQUEANTE -> "Tiene un tratamiento activo que impide el traslado.";
            case ADVERTENCIA -> "Tiene un tratamiento activo en curso.";
            case INFORMATIVA -> "Tiene un tratamiento activo en curso (informativo).";
        };
    }
}
