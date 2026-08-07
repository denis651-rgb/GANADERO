package bo.com.ganadero.movimientos.domain;

import java.util.UUID;

public record MovimientoAnimal(UUID animalId, long version) {
}
