package bo.com.ganadero.animales.api;
import bo.com.ganadero.animales.domain.AnimalResumen;
public record AnimalResumenResponse(long total,long activos,long hembras,long machos){public static AnimalResumenResponse from(AnimalResumen r){return new AnimalResumenResponse(r.total(),r.activos(),r.hembras(),r.machos());}}
