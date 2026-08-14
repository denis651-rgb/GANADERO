package bo.com.ganadero.reproduccion.application;
import bo.com.ganadero.animales.domain.SexoAnimal; import bo.com.ganadero.reproduccion.domain.*;
import java.math.BigDecimal; import java.time.*; import java.util.*;
public record RegistrarPartoCommand(UUID madreId,UUID diagnosticoGestacionId,UUID servicioId,LocalDate fechaParto,
 TipoParto tipoParto,DificultadParto dificultad,boolean asistido,UUID responsableId,String resultadoMadre,
 String observaciones,List<CriaCommand> crias) {
 public record CriaCommand(SexoAnimal sexo,BigDecimal pesoNacimientoKg,EstadoNacimiento estadoNacimiento,
  LocalTime horaNacimiento,String observaciones,boolean crearAnimal,String codigoAnimal,String nombreAnimal,UUID potreroInicialId) {}
}
