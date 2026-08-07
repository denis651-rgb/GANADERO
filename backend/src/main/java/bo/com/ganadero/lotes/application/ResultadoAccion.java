package bo.com.ganadero.lotes.application;

import java.util.UUID;

public record ResultadoAccion(UUID animalId, String estado, String mensaje) {
}
