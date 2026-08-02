package bo.com.ganadero.propiedades.api;
import bo.com.ganadero.propiedades.application.PropiedadCommand; import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record ActualizarPropiedadRequest(@Size(max=60) String codigo,@Size(max=160) String nombre,String descripcion,
 String departamento,String municipio,String localidad,String direccionReferencia,@PositiveOrZero BigDecimal superficieHa,
 String ubicacionWkt,String limiteGeograficoWkt,Boolean activo,@NotNull Long version){
 PropiedadCommand command(){return new PropiedadCommand(codigo,nombre,descripcion,departamento,municipio,localidad,direccionReferencia,superficieHa,ubicacionWkt,limiteGeograficoWkt,activo,version);}}
