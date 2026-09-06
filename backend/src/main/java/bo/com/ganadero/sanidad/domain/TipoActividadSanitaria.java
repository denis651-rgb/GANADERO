package bo.com.ganadero.sanidad.domain;

/**
 * Tipo de actividad sanitaria. Genérico a propósito: ninguna etiqueta de UI ni mensaje de dominio
 * debe asumir que la actividad es una vacunación sólo porque hoy la mayoría de los ejemplos lo son.
 */
public enum TipoActividadSanitaria {
    VACUNACION, DESPARASITACION, VITAMINIZACION, PRUEBA_DIAGNOSTICA,
    CONTROL_ECTOPARASITARIO, VIGILANCIA, TRATAMIENTO_PREVENTIVO, OTRA
}
