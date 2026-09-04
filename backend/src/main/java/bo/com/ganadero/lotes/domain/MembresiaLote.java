package bo.com.ganadero.lotes.domain;

import java.time.Instant;
import java.util.UUID;

public record MembresiaLote(
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
        long version,
        String animalCodigo,
        String animalNombre) {
    public MembresiaLote(UUID id, UUID loteId, UUID animalId, Instant fechaIngreso, Instant fechaSalida,
                        String motivoIngreso, String motivoSalida, String observacion, String modo,
                        UUID ingresadoPor, UUID salidaPor, long version) {
        this(id, loteId, animalId, fechaIngreso, fechaSalida, motivoIngreso, motivoSalida,
                observacion, modo, ingresadoPor, salidaPor, version, null, null);
    }
}
