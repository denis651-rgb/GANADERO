package bo.com.ganadero.alertas.application;

public record AlertaConfiguracion(
        int diasAlertaPreparto,
        int diasAlertaDestete,
        int diasDiagnosticoPostServicio,
        int diasGestacionEstimada) {
    public static AlertaConfiguracion valoresPredeterminados() {
        return new AlertaConfiguracion(15, 7, 30, 285);
    }
}
