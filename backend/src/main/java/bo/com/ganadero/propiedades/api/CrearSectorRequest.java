package bo.com.ganadero.propiedades.api;
import bo.com.ganadero.propiedades.application.SectorCommand; import jakarta.validation.constraints.*;
public record CrearSectorRequest(@Size(max=60) String codigo,@NotBlank @Size(max=160) String nombre,String descripcion){SectorCommand command(){return new SectorCommand(codigo,nombre,descripcion,true,0L);}}
