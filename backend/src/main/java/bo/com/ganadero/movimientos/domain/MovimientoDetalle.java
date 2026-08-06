package bo.com.ganadero.movimientos.domain;

import bo.com.ganadero.animales.domain.EstadoAnimal;

import java.util.UUID;

public record MovimientoDetalle(
        UUID id,
        UUID movimientoId,
        UUID animalId,
        EstadoAnimal estadoAntes,
        EstadoAnimal estadoDespues) {
}
