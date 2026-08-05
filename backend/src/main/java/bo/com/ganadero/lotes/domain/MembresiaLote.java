package bo.com.ganadero.lotes.domain;

import java.time.Instant;
import java.util.UUID;

public record MembresiaLote(
        UUID id,
        UUID loteId,
        UUID animalId,
        Instant fechaIngreso,
        Instant fechaSalida,
        String motivoSalida,
        UUID ingresadoPor,
        UUID salidaPor) {
}
