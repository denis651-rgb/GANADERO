package bo.com.ganadero.lotes.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IngresoLoteCommand(List<UUID> animalIds, String modo, Instant fechaIngreso,
                                 String motivo, String observacion) {
}
