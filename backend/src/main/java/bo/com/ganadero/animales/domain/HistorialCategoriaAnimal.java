package bo.com.ganadero.animales.domain;

import java.time.Instant;
import java.util.UUID;

/** Registro inmutable de cada cambio de categoría de un animal (nunca se sobreescribe ni elimina). */
public record HistorialCategoriaAnimal(UUID id, UUID animalId, UUID categoriaAnteriorId, UUID categoriaNuevaId,
                                       Instant fechaCambio, String tipoCambio, String motivo, UUID usuarioId,
                                       Long edadDias, boolean edadConfirmada, UUID categoriaConfigId) {
    public static final String AUTOMATICO = "AUTOMATICO";
    public static final String MANUAL = "MANUAL";
    public static final String CORRECCION = "CORRECCION";
}
