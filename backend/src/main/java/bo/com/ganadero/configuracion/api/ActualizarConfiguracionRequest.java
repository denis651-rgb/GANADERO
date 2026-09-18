package bo.com.ganadero.configuracion.api;
import bo.com.ganadero.configuracion.application.ConfiguracionCommand; import jakarta.validation.constraints.*;
public record ActualizarConfiguracionRequest(
 @Size(max=60) String zonaHoraria,@Size(max=10) String moneda,@Size(max=10) String unidadPeso,@Size(max=10) String unidadSuperficie,
 @Min(0) Integer diasAlertaPreparto,@Min(0) Integer diasSinPesaje,@Min(0) Integer diasAlertaDestete,
 @Min(0) Integer diasDiagnosticoPostServicio,@Min(1) Integer diasGestacionEstimada,Boolean comprimirImagenes,
 @Min(1) @Max(100) Integer calidadImagen,@Size(max=120) String nombreUsuario,
 @Size(min=4,max=20) String nuevoPin,Boolean quitarPin,@Min(0) Integer diasToleranciaPesoCompra,
 @Pattern(regexp="^([01]\\d|2[0-3]):[0-5]\\d$",message="Usa el formato HH:mm, por ejemplo 08:00.") String horaAvisos,@NotNull Long version){
 ConfiguracionCommand command(){return new ConfiguracionCommand(zonaHoraria,moneda,unidadPeso,unidadSuperficie,
  diasAlertaPreparto,diasSinPesaje,diasAlertaDestete,diasDiagnosticoPostServicio,
  diasGestacionEstimada,comprimirImagenes,calidadImagen,nombreUsuario,nuevoPin,quitarPin,diasToleranciaPesoCompra,horaAvisos,version);}}
