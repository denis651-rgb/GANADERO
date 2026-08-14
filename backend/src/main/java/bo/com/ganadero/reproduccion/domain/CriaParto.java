package bo.com.ganadero.reproduccion.domain;
import bo.com.ganadero.animales.domain.SexoAnimal; import java.math.BigDecimal; import java.time.LocalTime; import java.util.UUID;
public record CriaParto(UUID id,UUID empresaId,UUID partoId,UUID animalCriaId,SexoAnimal sexo,BigDecimal pesoNacimientoKg,
 EstadoNacimiento estadoNacimiento,LocalTime horaNacimiento,String observaciones,UUID clienteUuid,String idempotencyKey,
 String codigoAnimal,String nombreAnimal,long version) {}
