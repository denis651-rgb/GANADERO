package bo.com.ganadero.propiedades.api;
import bo.com.ganadero.propiedades.domain.Sector; import java.util.UUID;
public record SectorResponse(UUID id,UUID propiedadId,String codigo,String nombre,String descripcion,boolean activo,long version){public static SectorResponse from(Sector s){return new SectorResponse(s.id(),s.propiedadId(),s.codigo(),s.nombre(),s.descripcion(),s.activo(),s.version());}}
