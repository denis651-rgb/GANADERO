package bo.com.ganadero.sanidad.application;

/**
 * Se publica cuando cambia algo del plan sanitario que afecta al calendario (una actividad nueva,
 * editada, activada o desactivada, o un plan que pasa a ACTIVO). Al confirmarse la transacción,
 * {@link RegenerarCalendarioAlCambiarPlan} genera el calendario sin esperar a la corrida nocturna.
 */
record PlanSanitarioModificado() {
}
