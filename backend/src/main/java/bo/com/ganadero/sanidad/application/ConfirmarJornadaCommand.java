package bo.com.ganadero.sanidad.application;import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
public record ConfirmarJornadaCommand(UUID operationId,long version,UUID planItemId,UUID productoId,UUID loteProductoId,
 BigDecimal dosis,String unidadDosis,String viaAdministracion,LocalDate fechaAplicacion,String resultado,String observaciones){}
