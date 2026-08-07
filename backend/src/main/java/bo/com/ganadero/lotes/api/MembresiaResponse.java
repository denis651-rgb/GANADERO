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
        String motivoIngreso,
        String motivoSalida,
        String observacion,
        String modo,
        UUID ingresadoPor,
        UUID salidaPor,
        long version) {

    public static MembresiaResponse from(MembresiaLote m) {
        return new MembresiaResponse(m.id(), m.loteId(), m.animalId(), m.fechaIngreso(), m.fechaSalida(),
                m.motivoIngreso(), m.motivoSalida(), m.observacion(), m.modo(), m.ingresadoPor(), m.salidaPor(),
                m.version());
    }
}
