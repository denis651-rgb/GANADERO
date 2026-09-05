package bo.com.ganadero.sanidad.domain;

/**
 * Efecto explícito que un caso clínico o tratamiento tiene sobre el traslado del animal
 * (usado por "Mover lote"). Es un dato del dominio sanitario, fijado por el veterinario al
 * registrar el caso/tratamiento (con un valor por defecto calculado a partir de la severidad
 * o el estado si no lo especifica), no una inferencia hecha fuera de este módulo.
 */
public enum RestriccionMovimiento {
    BLOQUEANTE, ADVERTENCIA, INFORMATIVA
}
