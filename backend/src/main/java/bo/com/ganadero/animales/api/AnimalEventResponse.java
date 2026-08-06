package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.AnimalEvent;
import bo.com.ganadero.animales.domain.EstadoAnimal;

import java.time.Instant;
import java.util.UUID;

public record AnimalEventResponse(UUID id, String tipo, Instant fechaEvento, EstadoAnimal estadoAnterior,
                                  EstadoAnimal estadoNuevo, String motivo, UUID registradoPor,
                                  String titulo, String descripcion, String moduloOrigen, UUID registroOrigen,
                                  String dispositivo, String metadata, UUID createdBy) {
    static AnimalEventResponse from(AnimalEvent event) {
        return new AnimalEventResponse(event.id(), event.tipo(), event.fechaEvento(), event.estadoAnterior(),
                event.estadoNuevo(), event.motivo(), event.registradoPor(), event.titulo(), event.descripcion(),
                event.moduloOrigen(), event.registroOrigen(), event.dispositivo(), event.metadata(), event.createdBy());
    }
}
