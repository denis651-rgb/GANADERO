package bo.com.ganadero.animales.api; import bo.com.ganadero.animales.domain.Raza; import java.util.UUID;
public record RazaResponse(UUID id,String codigo,String nombre,String especie,String descripcion){public static RazaResponse from(Raza r){return new RazaResponse(r.id(),r.codigo(),r.nombre(),r.especie(),r.descripcion());}}
