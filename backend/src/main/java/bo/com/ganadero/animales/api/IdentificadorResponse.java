package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.EstadoIdentificador;
import bo.com.ganadero.animales.domain.IdentificadorAnimal;
import bo.com.ganadero.animales.domain.TipoIdentificador;

import java.time.Instant;
import java.util.UUID;

public record IdentificadorResponse(
        UUID id,
        UUID animalId,
        TipoIdentificador tipo,
        String valor,
        boolean principal,
        EstadoIdentificador estado,
        Instant fechaAsignacion,
        Instant fechaRetiro,
        String motivoRetiro,
        String observaciones,
        String payload,
        long version) {

    public static IdentificadorResponse from(IdentificadorAnimal i) {
        return new IdentificadorResponse(i.id(), i.animalId(), i.tipo(), i.valor(), i.principal(), i.estado(),
                i.fechaAsignacion(), i.fechaRetiro(), i.motivoRetiro(), i.observaciones(), i.payload(), i.version());
    }
}
