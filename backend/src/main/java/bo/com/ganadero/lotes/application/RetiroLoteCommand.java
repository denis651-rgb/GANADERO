package bo.com.ganadero.lotes.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RetiroLoteCommand(List<UUID> animalIds, Instant fechaSalida, String motivo) {
}
