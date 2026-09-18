package bo.com.ganadero.alertas.application;

import java.time.LocalTime;

public record AlertaConfiguracion(
        int diasAlertaPreparto,
        int diasAlertaDestete,
        int diasDiagnosticoPostServicio,
        int diasGestacionEstimada,
        LocalTime horaAvisos) {
    /** Hora a la que sale un aviso que nace de una fecha sin hora propia, si nadie configuró otra. */
    public static final LocalTime HORA_AVISOS_PREDETERMINADA = LocalTime.of(8, 0);

    public AlertaConfiguracion {
        if (horaAvisos == null) horaAvisos = HORA_AVISOS_PREDETERMINADA;
    }

    public AlertaConfiguracion(int diasAlertaPreparto, int diasAlertaDestete, int diasDiagnosticoPostServicio,
                               int diasGestacionEstimada) {
        this(diasAlertaPreparto, diasAlertaDestete, diasDiagnosticoPostServicio, diasGestacionEstimada,
                HORA_AVISOS_PREDETERMINADA);
    }

    public static AlertaConfiguracion valoresPredeterminados() {
        return new AlertaConfiguracion(15, 7, 30, 285);
    }
}
