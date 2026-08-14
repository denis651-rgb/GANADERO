package bo.com.ganadero.alertas.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato de entrada del futuro Motor de Alertas (Fase 3.6).
 * La fecha programada siempre llega calculada por el módulo de negocio.
 */
public record ProgramarAlertaCommand(
        UUID empresaId,
        UUID animalId,
        TipoAlerta tipo,
        Instant fechaProgramada,
        String origenTipo,
        UUID origenId,
        Map<String, Object> metadata) {
    public ProgramarAlertaCommand(UUID empresaId,UUID animalId,TipoAlerta tipo,Instant fechaProgramada,Instant fechaVencimiento,
                                  String origenTipo,UUID origenId,Map<String,Object> metadata){
        this(empresaId,animalId,tipo,fechaProgramada,origenTipo,origenId,new java.util.HashMap<>(metadata));
        this.metadata().put("fechaVencimiento",fechaVencimiento.toString());
    }
}
