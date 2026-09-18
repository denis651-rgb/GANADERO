package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.SeveridadAlerta;

/**
 * Cuánta atención pide un grupo de alertas, en tres niveles. Es lo que el módulo de alertas expone a
 * los demás módulos (por ejemplo el dashboard) en lugar de su escala interna de severidades, que
 * queda como detalle propio del módulo. El orden de las constantes va de menos a más urgente.
 */
public enum NivelAtencion {
    INFORMATIVO, ADVERTENCIA, URGENTE;

    /** Lo crítico y lo urgente piden atención urgente; las advertencias y lo informativo conservan su nivel. */
    static NivelAtencion de(SeveridadAlerta severidad) {
        return switch (severidad) {
            case CRITICA, URGENTE -> URGENTE;
            case WARNING -> ADVERTENCIA;
            case INFO -> INFORMATIVO;
        };
    }
}
