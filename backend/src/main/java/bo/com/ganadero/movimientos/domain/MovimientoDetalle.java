package bo.com.ganadero.movimientos.domain;

import bo.com.ganadero.animales.domain.EstadoAnimal;

import java.util.UUID;

public record MovimientoDetalle(
        UUID id,
        UUID movimientoId,
        UUID animalId,
        long animalVersionEsperada,
        EstadoAnimal estadoAntes,
        EstadoAnimal estadoDespues,
        UUID propiedadAntes,
        UUID potreroAntes,
        UUID loteAntes,
        UUID propiedadDespues,
        UUID potreroDespues,
        UUID loteDespues,
        String estadoResultado,
        String mensajeResultado) {

    public MovimientoDetalle {
        if (estadoAntes == null) estadoAntes = EstadoAnimal.ACTIVO;
        if (estadoDespues == null) estadoDespues = EstadoAnimal.ACTIVO;
    }
}
