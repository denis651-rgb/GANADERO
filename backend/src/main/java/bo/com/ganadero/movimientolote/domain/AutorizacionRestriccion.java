package bo.com.ganadero.movimientolote.domain;

import java.time.Instant;
import java.util.UUID;

/** Excepción registrada al aceptar una restricción de severidad ADVERTENCIA para poder continuar. */
public record AutorizacionRestriccion(UUID id, UUID preparacionId, UUID animalId, String tipoRestriccion,
                                      String motivo, UUID usuarioId, Instant fecha) {
}
