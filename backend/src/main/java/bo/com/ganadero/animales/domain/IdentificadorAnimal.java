package bo.com.ganadero.animales.domain;

import java.time.Instant;
import java.util.UUID;

public record IdentificadorAnimal(
        UUID id,
        UUID empresaId,
        UUID animalId,
        TipoIdentificador tipo,
        String valor,
        boolean principal,
        EstadoIdentificador estado,
        Instant fechaAsignacion,
        Instant fechaRetiro,
        String motivoRetiro,
        UUID asignadoPor,
        UUID retiradoPor,
        String observaciones,
        long version) {
}
