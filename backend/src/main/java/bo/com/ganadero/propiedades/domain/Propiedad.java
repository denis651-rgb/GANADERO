package bo.com.ganadero.propiedades.domain;
import java.math.BigDecimal; import java.util.UUID;

/** Catalogo de propiedades/parcelas de la operacion (app de un solo usuario, sin multi-tenant). */
public record Propiedad(UUID id, String codigo, String nombre, String descripcion, String departamento,
 String municipio, String localidad, String direccionReferencia, BigDecimal superficieHa,
 String ubicacionWkt, String limiteGeograficoWkt, boolean activo, long version) {}
