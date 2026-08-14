package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.reproduccion.domain.MetodoDiagnostico;
import bo.com.ganadero.reproduccion.domain.ResultadoGestacion;

import java.time.Instant;
import java.util.UUID;

public record RegistrarDiagnosticoCommand(
        UUID id,
        UUID animalId,
        UUID servicioId,
        Instant fechaDiagnostico,
        ResultadoGestacion resultado,
        MetodoDiagnostico metodo,
        Integer diasGestacionEstimados,
        UUID veterinarioId,
        String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        String idempotencyKey) {
    public RegistrarDiagnosticoCommand(UUID id, UUID animalId, UUID servicioId,
                                       java.time.LocalDate fechaDiagnostico, ResultadoGestacion resultado,
                                       MetodoDiagnostico metodo, String observaciones, UUID propiedadId,
                                       UUID potreroId, UUID loteId, UUID clienteUuid, String idempotencyKey) {
        this(id, animalId, servicioId, fechaDiagnostico.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                resultado, metodo, null, null, observaciones, propiedadId, potreroId, loteId,
                clienteUuid, idempotencyKey);
    }
}
