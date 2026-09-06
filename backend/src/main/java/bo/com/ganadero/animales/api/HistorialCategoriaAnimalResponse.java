package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.HistorialCategoriaAnimal;

import java.time.Instant;
import java.util.UUID;

public record HistorialCategoriaAnimalResponse(UUID id, UUID categoriaAnteriorId, UUID categoriaNuevaId,
                                               Instant fechaCambio, String tipoCambio, String motivo,
                                               UUID usuarioId, Long edadDias, boolean edadConfirmada) {
    public static HistorialCategoriaAnimalResponse from(HistorialCategoriaAnimal h) {
        return new HistorialCategoriaAnimalResponse(h.id(), h.categoriaAnteriorId(), h.categoriaNuevaId(),
                h.fechaCambio(), h.tipoCambio(), h.motivo(), h.usuarioId(), h.edadDias(), h.edadConfirmada());
    }
}
