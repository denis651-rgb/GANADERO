package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;

import java.util.List;
import java.util.UUID;

public record AnimalElegibilidad(
        UUID id,
        String codigo,
        String nombre,
        SexoAnimal sexo,
        EstadoAnimal estado,
        Long edadDias,
        boolean elegible,
        List<String> motivos
) {
}
