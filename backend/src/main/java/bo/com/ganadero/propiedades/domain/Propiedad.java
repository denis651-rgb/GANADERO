package bo.com.ganadero.propiedades.domain;
import java.math.BigDecimal; import java.util.UUID;
public record Propiedad(UUID id,UUID empresaId,String codigo,String nombre,String descripcion,String departamento,
 String municipio,String localidad,String direccionReferencia,BigDecimal superficieHa,String ubicacionWkt,
 String limiteGeograficoWkt,boolean activo,long version) {}
