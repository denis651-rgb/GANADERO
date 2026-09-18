package bo.com.ganadero.alertas.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato de entrada del Motor de Alertas. La fecha programada llega calculada por el módulo de
 * negocio, salvo en los avisos «para un día» (ver {@link #alDia}), donde el módulo indica el día y
 * el motor decide la hora.
 */
public record ProgramarAlertaCommand(
        UUID empresaId,
        UUID animalId,
        TipoAlerta tipo,
        Instant fechaProgramada,
        String origenTipo,
        UUID origenId,
        Map<String, Object> metadata) {
    /** Clave reservada de la metadata con la que un aviso pide salir a la hora de avisos configurada. */
    static final String CLAVE_DIA_DE_AVISO = "diaDeAviso";

    public ProgramarAlertaCommand(UUID empresaId,UUID animalId,TipoAlerta tipo,Instant fechaProgramada,Instant fechaVencimiento,
                                  String origenTipo,UUID origenId,Map<String,Object> metadata){
        this(empresaId,animalId,tipo,fechaProgramada,origenTipo,origenId,new java.util.HashMap<>(metadata));
        this.metadata().put("fechaVencimiento",fechaVencimiento.toString());
    }

    /**
     * Aviso que nace de una fecha sin hora propia (parto probable, destete, fin de un retiro…): el
     * motor lo programa el {@code dia} indicado a la hora de avisos configurada, en vez de a
     * medianoche, que es una hora a la que nadie lo ve. {@code fechaVencimiento} puede ser nula.
     */
    public static ProgramarAlertaCommand alDia(UUID empresaId, UUID animalId, TipoAlerta tipo, LocalDate dia,
                                               Instant fechaVencimiento, String origenTipo, UUID origenId,
                                               Map<String, Object> metadata) {
        Map<String, Object> datos = new java.util.HashMap<>(metadata);
        datos.put(CLAVE_DIA_DE_AVISO, dia.toString());
        if (fechaVencimiento != null) datos.put("fechaVencimiento", fechaVencimiento.toString());
        // Instante provisional (medianoche): el motor lo reemplaza por la hora de avisos.
        Instant provisional = dia.atStartOfDay(ZoneId.of("America/La_Paz")).toInstant();
        return new ProgramarAlertaCommand(empresaId, animalId, tipo, provisional, origenTipo, origenId, datos);
    }
}
