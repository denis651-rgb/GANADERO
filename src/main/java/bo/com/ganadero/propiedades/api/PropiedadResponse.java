package bo.com.ganadero.propiedades.api;
import bo.com.ganadero.propiedades.domain.Propiedad; import java.math.BigDecimal; import java.util.UUID;
public record PropiedadResponse(UUID id,String codigo,String nombre,String descripcion,String departamento,String municipio,
 String localidad,String direccionReferencia,BigDecimal superficieHa,String ubicacionWkt,String limiteGeograficoWkt,
 boolean activo,long version){public static PropiedadResponse from(Propiedad p){return new PropiedadResponse(p.id(),p.codigo(),p.nombre(),p.descripcion(),p.departamento(),p.municipio(),p.localidad(),p.direccionReferencia(),p.superficieHa(),p.ubicacionWkt(),p.limiteGeograficoWkt(),p.activo(),p.version());}}
