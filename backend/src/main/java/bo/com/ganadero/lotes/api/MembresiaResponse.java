package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.domain.MembresiaLote;

import java.time.Instant;
import java.util.UUID;

public record MembresiaResponse(
        UUID id,
        UUID loteId,
        UUID animalId,
        Instant fechaIngreso,
        Instant fechaSalida,
        String motivoSalida) {

    public static MembresiaResponse from(MembresiaLote m) {
        return new MembresiaResponse(m.id(), m.loteId(), m.animalId(), m.fechaIngreso(), m.fechaSalida(), m.motivoSalida());
    }
}
