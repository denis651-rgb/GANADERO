package bo.com.ganadero.sanidad.application; import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public record RegistrarAplicacionDeclaradaCommand(UUID animalId,TipoActividadSanitaria tipoActividad,UUID planItemId,
 LocalDate fechaAplicacion,BigDecimal dosis,String unidadDosis,String productoTexto,String observaciones) {}
