package bo.com.ganadero.alertas.application;

import java.util.UUID;
import java.util.Set;

/** Puerto público que implementará el Motor de Alertas en la Fase 3.6. */
public interface MotorAlertas {
    UUID programar(ProgramarAlertaCommand command);

    UUID evolucionar(ProgramarAlertaCommand command, Set<TipoAlerta> tiposAnteriores);

    void cancelarPorOrigen(UUID empresaId, String origenTipo, UUID origenId, String motivo);

    void resolverPorOrigen(UUID empresaId, String origenTipo, UUID origenId);

    default UUID crearInmediata(ProgramarAlertaCommand command) { return programar(command); }
}
