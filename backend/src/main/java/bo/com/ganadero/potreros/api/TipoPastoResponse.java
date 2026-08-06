package bo.com.ganadero.potreros.api; import bo.com.ganadero.potreros.domain.TipoPasto; import java.util.UUID;
public record TipoPastoResponse(UUID id,String codigo,String nombre,String nombreCientifico,String descripcion){public static TipoPastoResponse from(TipoPasto t){return new TipoPastoResponse(t.id(),t.codigo(),t.nombre(),t.nombreCientifico(),t.descripcion());}}
