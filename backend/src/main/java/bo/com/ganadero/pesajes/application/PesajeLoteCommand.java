package bo.com.ganadero.pesajes.application;

import java.time.LocalDate;
import java.util.UUID;

public record PesajeLoteCommand(
        UUID id,
        UUID loteId,
        LocalDate fecha,
        java.math.BigDecimal pesoKg,
        String dispositivo,
        UUID clienteUuid,
        String idempotencyKey,
        String observaciones) {
}
