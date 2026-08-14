package bo.com.ganadero.sanidad.domain;import java.math.BigDecimal;import java.util.UUID;
public record TratamientoDetalle(UUID id,UUID empresaId,UUID tratamientoId,UUID productoId,UUID loteProductoId,BigDecimal dosis,String unidadDosis,int frecuenciaHoras,int duracionDias,String viaAdministracion,int retiroCarneDias,int retiroLecheDias){}
