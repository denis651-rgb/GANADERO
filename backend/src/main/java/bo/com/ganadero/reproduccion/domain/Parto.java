package bo.com.ganadero.reproduccion.domain;
import java.time.*; import java.util.UUID;
public record Parto(UUID id,UUID empresaId,UUID madreId,UUID diagnosticoGestacionId,UUID servicioId,LocalDate fechaParto,
 TipoParto tipoParto,DificultadParto dificultad,boolean asistido,UUID responsableId,String resultadoMadre,int numeroCrias,
 String observaciones,UUID propiedadId,UUID potreroId,UUID loteId,UUID clienteUuid,String idempotencyKey,
 EstadoRegistroReproduccion estado,Instant anuladoAt,UUID anuladoBy,String motivoAnulacion,String codigoMadre,String nombreMadre,
 UUID machoId,String codigoMacho,String nombreMacho,String potreroNombre,String propiedadNombre,long version) {}
