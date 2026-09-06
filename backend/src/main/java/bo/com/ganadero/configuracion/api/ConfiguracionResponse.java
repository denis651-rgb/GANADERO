package bo.com.ganadero.configuracion.api;
import bo.com.ganadero.configuracion.domain.Configuracion;
public record ConfiguracionResponse(String zonaHoraria,String moneda,String unidadPeso,String unidadSuperficie,
 int diasAlertaPreparto,int diasAlertaVacunacion,int diasSinPesaje,int diasAlertaDestete,
 int diasDiagnosticoPostServicio,int diasGestacionEstimada,boolean comprimirImagenes,int calidadImagen,
 String nombreUsuario,boolean pinConfigurado,int diasToleranciaPesoCompra,long version){
 public static ConfiguracionResponse from(Configuracion c){return new ConfiguracionResponse(c.zonaHoraria(),c.moneda(),
  c.unidadPeso(),c.unidadSuperficie(),c.diasAlertaPreparto(),c.diasAlertaVacunacion(),c.diasSinPesaje(),
  c.diasAlertaDestete(),c.diasDiagnosticoPostServicio(),c.diasGestacionEstimada(),c.comprimirImagenes(),
  c.calidadImagen(),c.nombreUsuario(),c.pinConfigurado(),c.diasToleranciaPesoCompra(),c.version());}}
