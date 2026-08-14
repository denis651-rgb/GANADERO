package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.reproduccion.domain.IntensidadCelo;
import bo.com.ganadero.reproduccion.domain.TipoCelo;

import java.time.Instant;
import java.util.UUID;

public record RegistrarCeloCommand(
        UUID id,
        UUID animalId,
        Instant fechaDeteccion,
        TipoCelo tipoDeteccion,
        IntensidadCelo intensidad,
        String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        String idempotencyKey) {
    public RegistrarCeloCommand(UUID id, UUID animalId, java.time.LocalDate fechaDeteccion,
                                TipoCelo tipoDeteccion, String observaciones, UUID propiedadId,
                                UUID potreroId, UUID loteId, UUID clienteUuid, String idempotencyKey) {
        this(id, animalId, fechaDeteccion.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), tipoDeteccion,
                null, observaciones, propiedadId, potreroId, loteId, clienteUuid, idempotencyKey);
    }
}
