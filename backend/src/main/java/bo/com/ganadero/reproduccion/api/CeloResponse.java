package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.Celo;
import bo.com.ganadero.reproduccion.domain.EstadoRegistroReproduccion;
import bo.com.ganadero.reproduccion.domain.IntensidadCelo;
import bo.com.ganadero.reproduccion.domain.TipoCelo;

import java.time.Instant;
import java.util.UUID;

public record CeloResponse(
        UUID id,
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        Instant fechaDeteccion,
        TipoCelo tipoDeteccion,
        IntensidadCelo intensidad,
        String observaciones,
        UUID propiedadId,
        String propiedadNombre,
        UUID potreroId,
        String potreroNombre,
        UUID loteId,
        UUID clienteUuid,
        EstadoRegistroReproduccion estado,
        long version) {

    public static CeloResponse from(Celo c) {
        return new CeloResponse(c.id(), c.animalId(), c.codigoAnimal(), c.nombreAnimal(), c.fechaDeteccion(),
                c.tipoDeteccion(), c.intensidad(), c.observaciones(), c.propiedadId(), c.propiedadNombre(),
                c.potreroId(), c.potreroNombre(), c.loteId(), c.clienteUuid(), c.estado(), c.version());
    }
}
