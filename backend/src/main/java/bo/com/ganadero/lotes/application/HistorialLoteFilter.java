package bo.com.ganadero.lotes.application;

import java.time.Instant;
import java.util.UUID;

public record HistorialLoteFilter(UUID animalId, Instant desde, Instant hasta,
                                  String motivoIngreso, String motivoSalida, int page, int size) {
}
