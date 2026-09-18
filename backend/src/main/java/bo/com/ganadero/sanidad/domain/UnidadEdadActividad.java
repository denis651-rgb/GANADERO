package bo.com.ganadero.sanidad.domain;

/** Unidad en la que se configuró la edad de una actividad (el almacenamiento interno siempre es en días). */
public enum UnidadEdadActividad {
    DIAS, MESES, ANIOS;

    /** Convierte a días con la misma equivalencia en todo el sistema (mes = 30 días, año = 365). */
    public int aDias(int valor) {
        return switch (this) {
            case DIAS -> valor;
            case MESES -> valor * 30;
            case ANIOS -> valor * 365;
        };
    }
}
