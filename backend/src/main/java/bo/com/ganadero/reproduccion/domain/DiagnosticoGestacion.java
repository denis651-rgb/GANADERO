package bo.com.ganadero.reproduccion.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DiagnosticoGestacion(
        UUID id,
        UUID empresaId,
        UUID animalId,
        UUID servicioId,
        Instant fechaDiagnostico,
        ResultadoGestacion resultado,
        MetodoDiagnostico metodo,
        Integer diasGestacionEstimados,
        LocalDate fechaProbableParto,
        UUID veterinarioId,
        String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        String idempotencyKey,
        EstadoRegistroReproduccion estado,
        String codigoAnimal,
        String nombreAnimal,
        String potreroNombre,
        String propiedadNombre,
        long version) {
}
