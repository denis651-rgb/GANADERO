package bo.com.ganadero.alertas.api;
import bo.com.ganadero.alertas.application.CrearRecordatorioCommand;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public record CrearRecordatorioRequest(@NotBlank @Size(max=200)String titulo,@NotBlank @Size(max=1000)String mensaje,
 @NotNull SeveridadAlerta severidad,UUID animalId,@NotNull @Future Instant fechaEvento,@NotNull Instant primeraNotificacion,
 @Min(1) @Max(10)int cantidadNotificaciones,@Min(15)Integer intervaloMinutos){
 CrearRecordatorioCommand command(){return new CrearRecordatorioCommand(titulo,mensaje,severidad,animalId,fechaEvento,primeraNotificacion,cantidadNotificaciones,intervaloMinutos);}
}
