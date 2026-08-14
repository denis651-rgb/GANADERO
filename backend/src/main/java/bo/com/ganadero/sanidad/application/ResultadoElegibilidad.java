package bo.com.ganadero.sanidad.application;

import java.util.List;

public record ResultadoElegibilidad(
        List<AnimalElegibilidad> elegibles,
        List<AnimalElegibilidad> noElegibles
) {
    public static ResultadoElegibilidad of(List<AnimalElegibilidad> animales) {
        return new ResultadoElegibilidad(
                animales.stream().filter(AnimalElegibilidad::elegible).toList(),
                animales.stream().filter(animal -> !animal.elegible()).toList()
        );
    }
}
