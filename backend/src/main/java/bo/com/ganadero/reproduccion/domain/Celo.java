package bo.com.ganadero.reproduccion.domain;

import java.time.Instant;
import java.util.UUID;

public record Celo(
        UUID id,
        UUID empresaId,
        UUID animalId,
        Instant fechaDeteccion,
        TipoCelo tipoDeteccion,
        IntensidadCelo intensidad,
        UUID detectadoPor,
        String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        String idempotencyKey,
        EstadoRegistroReproduccion estado,
        Instant anuladoAt,
        UUID anuladoBy,
        String motivoAnulacion,
        String codigoAnimal,
        String nombreAnimal,
        String potreroNombre,
        String propiedadNombre,
        long version) {
    public Celo(UUID id, UUID empresaId, UUID animalId, java.time.LocalDate fechaDeteccion,
                TipoCelo tipoDeteccion, String observaciones, UUID propiedadId, UUID potreroId,
                UUID loteId, UUID clienteUuid, String idempotencyKey, EstadoRegistroReproduccion estado,
                String codigoAnimal, String nombreAnimal, String potreroNombre, String propiedadNombre,
                long version) {
        this(id, empresaId, animalId, fechaDeteccion.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                tipoDeteccion, null, null, observaciones, propiedadId, potreroId, loteId, clienteUuid,
                idempotencyKey, estado, null, null, null, codigoAnimal, nombreAnimal, potreroNombre,
                propiedadNombre, version);
    }
}
