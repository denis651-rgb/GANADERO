package bo.com.ganadero.sanidad.application;
import java.math.BigDecimal;import java.time.Instant;import java.util.*;
public record CrearTratamientoCommand(UUID casoClinicoId,UUID animalId,Instant fechaInicio,Instant fechaFinEstimada,String diagnostico,UUID veterinarioId,String observaciones,List<Detalle> detalles){public record Detalle(UUID productoId,UUID loteProductoId,BigDecimal dosis,String unidadDosis,Integer frecuenciaHoras,Integer duracionDias,String viaAdministracion,Integer retiroCarneDias,Integer retiroLecheDias){}}
