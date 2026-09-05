package bo.com.ganadero.movimientolote.application;

import java.util.UUID;

public record AutorizacionCommand(UUID animalId, String tipoRestriccion, String motivo) {
}
