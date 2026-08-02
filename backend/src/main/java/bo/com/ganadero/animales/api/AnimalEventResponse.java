package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.AnimalEvent;
import bo.com.ganadero.animales.domain.EstadoAnimal;

import java.time.Instant;
import java.util.UUID;

public record AnimalEventResponse(UUID id, String tipo, Instant fechaEvento, EstadoAnimal estadoAnterior,
                                  EstadoAnimal estadoNuevo, String motivo, UUID registradoPor) {
    static AnimalEventResponse from(AnimalEvent event) {
        return new AnimalEventResponse(event.id(), event.tipo(), event.fechaEvento(), event.estadoAnterior(),
                event.estadoNuevo(), event.motivo(), event.registradoPor());
    }
}
