package bo.com.ganadero.reproduccion.api;
import bo.com.ganadero.animales.domain.SexoAnimal; import bo.com.ganadero.reproduccion.application.RegistrarPartoCommand; import bo.com.ganadero.reproduccion.domain.*;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.*; import java.util.*;
public record RegistrarPartoRequest(@NotNull UUID madreId,UUID diagnosticoGestacionId,UUID servicioId,@NotNull @PastOrPresent LocalDate fechaParto,
 @NotNull TipoParto tipoParto,@NotNull DificultadParto dificultad,boolean asistido,UUID responsableId,@Size(max=30) String resultadoMadre,
 @Size(max=1000) String observaciones,@NotEmpty List<@Valid CriaRequest> crias){
 public record CriaRequest(@NotNull SexoAnimal sexo,@Positive BigDecimal pesoNacimientoKg,@NotNull EstadoNacimiento estadoNacimiento,
  LocalTime horaNacimiento,@Size(max=1000) String observaciones,boolean crearAnimal,@Size(max=60) String codigoAnimal,
  @Size(max=160) String nombreAnimal,UUID potreroInicialId){}
 RegistrarPartoCommand command(){return new RegistrarPartoCommand(madreId,diagnosticoGestacionId,servicioId,fechaParto,tipoParto,dificultad,
  asistido,responsableId,resultadoMadre,observaciones,crias.stream().map(c->new RegistrarPartoCommand.CriaCommand(c.sexo,c.pesoNacimientoKg,
  c.estadoNacimiento,c.horaNacimiento,c.observaciones,c.crearAnimal,c.codigoAnimal,c.nombreAnimal,c.potreroInicialId)).toList());}
}
