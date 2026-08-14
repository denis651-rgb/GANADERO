package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.DiagnosticoGestacion;
import bo.com.ganadero.reproduccion.domain.EstadoRegistroReproduccion;
import bo.com.ganadero.reproduccion.domain.MetodoDiagnostico;
import bo.com.ganadero.reproduccion.domain.ResultadoGestacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DiagnosticoGestacionResponse(
        UUID id,
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        UUID servicioId,
        Instant fechaDiagnostico,
        ResultadoGestacion resultado,
        MetodoDiagnostico metodo,
        Integer diasGestacionEstimados,
        LocalDate fechaProbableParto,
        UUID veterinarioId,
        String observaciones,
        UUID propiedadId,
        String propiedadNombre,
        UUID potreroId,
        String potreroNombre,
        UUID loteId,
        UUID clienteUuid,
        EstadoRegistroReproduccion estado,
        long version) {

    public static DiagnosticoGestacionResponse from(DiagnosticoGestacion d) {
        return new DiagnosticoGestacionResponse(d.id(), d.animalId(), d.codigoAnimal(), d.nombreAnimal(),
                d.servicioId(), d.fechaDiagnostico(), d.resultado(), d.metodo(), d.diasGestacionEstimados(),
                d.fechaProbableParto(), d.veterinarioId(),
                d.observaciones(), d.propiedadId(), d.propiedadNombre(), d.potreroId(), d.potreroNombre(),
                d.loteId(), d.clienteUuid(), d.estado(), d.version());
    }
}
