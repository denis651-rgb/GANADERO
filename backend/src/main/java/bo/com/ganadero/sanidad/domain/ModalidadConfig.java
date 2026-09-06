package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.util.List;

/**
 * Configuración específica de la modalidad de una actividad sanitaria (secciones 10-14). Se
 * persiste como JSON en {@code plan_sanitario_item.modalidad_config}; el subtipo concreto se
 * decide por la columna hermana {@code modalidad}, no por información de tipo embebida en el
 * propio JSON (evita depender de polimorfismo Jackson para algo que ya es sabido por columna).
 */
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
