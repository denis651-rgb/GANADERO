package bo.com.ganadero.propiedades.domain;
import java.util.UUID;
public record Sector(UUID id,UUID empresaId,UUID propiedadId,String codigo,String nombre,String descripcion,
 boolean activo,long version) {}
