package bo.com.ganadero.movimientolote.domain;

public enum SeveridadRestriccion {
    /** Impide el movimiento; no puede omitirse desde el frontend. */
    BLOQUEANTE,
    /** Permite continuar solo con autorización explícita (usuario + motivo). */
    ADVERTENCIA,
    /** Solo se muestra; nunca bloquea ni requiere autorización. */
    INFORMATIVA
}
