package bo.com.ganadero.configuracion.application;
public record ConfiguracionCommand(String zonaHoraria, String moneda, String unidadPeso, String unidadSuperficie,
 Integer diasAlertaPreparto, Integer diasAlertaVacunacion, Integer diasSinPesaje, Integer diasAlertaDestete,
 Integer diasDiagnosticoPostServicio, Integer diasGestacionEstimada, Boolean comprimirImagenes, Integer calidadImagen,
 String nombreUsuario, String nuevoPin, Boolean quitarPin, Long version) {}
