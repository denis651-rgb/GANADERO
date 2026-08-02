package bo.com.ganadero.propiedades.api;
import bo.com.ganadero.propiedades.application.SectorCommand; import jakarta.validation.constraints.*;
public record ActualizarSectorRequest(@Size(max=60) String codigo,@Size(max=160) String nombre,String descripcion,Boolean activo,@NotNull Long version){SectorCommand command(){return new SectorCommand(codigo,nombre,descripcion,activo,version);}}
