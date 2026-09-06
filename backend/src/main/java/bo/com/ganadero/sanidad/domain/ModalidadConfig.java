package bo.com.ganadero.sanidad.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.List;

/**
 * Configuración específica de la modalidad de una actividad sanitaria (secciones 10-14). Se
 * persiste como JSON en {@code plan_sanitario_item.modalidad_config}. Para las solicitudes HTTP,
 * Jackson deduce el subtipo por sus campos exclusivos sin exigir un discriminador adicional en
 * el contrato; al leer desde la base, el repositorio continúa usando la columna {@code modalidad}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(ModalidadConfig.PorEdadConfig.class),
        @JsonSubTypes.Type(ModalidadConfig.PeriodicaConfig.class),
        @JsonSubTypes.Type(ModalidadConfig.FechaProgramadaConfig.class),
        @JsonSubTypes.Type(ModalidadConfig.PorHallazgoConfig.class),
        @JsonSubTypes.Type(ModalidadConfig.ManualConfig.class)
})
public sealed interface ModalidadConfig {

    record PorEdadConfig(int edadObjetivoValor, UnidadEdadActividad edadUnidad, int ventanaAnticipadaDias,
                        int ventanaPosteriorDias, PoliticaEdadEstimada politicaEdadEstimada,
                        PoliticaEdadDesconocida politicaEdadDesconocida, boolean unaVezEnLaVida)
            implements ModalidadConfig {
    }

    record PeriodicaConfig(int frecuenciaValor, UnidadFrecuencia frecuenciaUnidad,
                          ReferenciaCalculoPeriodica referenciaCalculo, int toleranciaAnticipadaDias,
                          int toleranciaPosteriorDias) implements ModalidadConfig {
    }

    record FechaProgramadaConfig(Instant fechaProgramada, boolean unicaVez, String reglaRepeticion,
                                 String zonaHoraria, Integer ventanaEjecucionHoras) implements ModalidadConfig {
    }

    record PorHallazgoConfig(List<String> tiposHallazgo, String severidadMinima, String accionRecomendada,
                             Integer plazoDias, boolean requiereValidacionVeterinaria) implements ModalidadConfig {
    }

    record ManualConfig() implements ModalidadConfig {
    }
}
