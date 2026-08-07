package bo.com.ganadero.movimientos.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.shared.error.ErrorCode;

import java.util.UUID;

public record ValidacionAnimalResult(UUID animalId, boolean valido, ErrorCode error, String mensaje, Animal animal) {
    public static ValidacionAnimalResult valid(Animal animal) {
        return new ValidacionAnimalResult(animal.id(), true, null, null, animal);
    }

    public static ValidacionAnimalResult invalid(UUID animalId, ErrorCode error, String mensaje) {
        return new ValidacionAnimalResult(animalId, false, error, mensaje, null);
    }
}
