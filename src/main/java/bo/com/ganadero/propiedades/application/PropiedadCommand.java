package bo.com.ganadero.propiedades.application;
import java.math.BigDecimal;
public record PropiedadCommand(String codigo,String nombre,String descripcion,String departamento,String municipio,
 String localidad,String direccionReferencia,BigDecimal superficieHa,String ubicacionWkt,String limiteGeograficoWkt,
 Boolean activo,Long version) {}
