package bo.com.ganadero.animales.domain;

import java.time.Instant;
import java.util.UUID;

public record AnimalEvent(
        UUID id,
        String tipo,
        Instant fechaEvento,
        EstadoAnimal estadoAnterior,
        EstadoAnimal estadoNuevo,
        String motivo,
        UUID registradoPor,
        String titulo,
        String descripcion,
        String moduloOrigen,
        UUID registroOrigen,
        String dispositivo,
        String metadata,
        UUID createdBy) {
}
