package bo.com.ganadero.configuracion.api;
import bo.com.ganadero.configuracion.domain.Configuracion;
public record ConfiguracionResponse(
    String zonaHoraria,String moneda,String unidadPeso,String unidadSuperficie,
    int diasAlertaPreparto,int diasAlertaPreparto2,int diasAlertaPreparto3,int diasAlertaVacunacion,
    int diasSinPesaje,int diasAlertaDestete,
    int diasDiagnosticoPostServicio,int diasGestacionEstimada,boolean comprimirImagenes,int calidadImagen,
    String nombreUsuario,boolean pinConfigurado,int diasToleranciaPesoCompra,String horaAvisos,long version){
    public static ConfiguracionResponse from(Configuracion c){return new ConfiguracionResponse(
        c.zonaHoraria(),c.moneda(),c.unidadPeso(),c.unidadSuperficie(),
        c.diasAlertaPreparto() != null ? c.diasAlertaPreparto() : 15,
        c.diasAlertaPreparto2() != null ? c.diasAlertaPreparto2() : 7,
        c.diasAlertaPreparto3() != null ? c.diasAlertaPreparto3() : 1,
        c.diasAlertaVacunacion() != null ? c.diasAlertaVacunacion() : 1,
        c.diasSinPesaje() != null ? c.diasSinPesaje() : 60,
        c.diasAlertaDestete() != null ? c.diasAlertaDestete() : 7,
        c.diasDiagnosticoPostServicio() != null ? c.diasDiagnosticoPostServicio() : 30,
        c.diasGestacionEstimada() != null ? c.diasGestacionEstimada() : 285,
        c.comprimirImagenes() != null ? c.comprimirImagenes() : true,
        c.calidadImagen() != null ? c.calidadImagen() : 80,
        c.nombreUsuario(),c.pinConfigurado(),
        c.diasToleranciaPesoCompra() != null ? c.diasToleranciaPesoCompra() : 5,
        c.horaAvisos() != null ? c.horaAvisos() : "07:00",c.version());}}
