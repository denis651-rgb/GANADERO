package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.application.PesajeLoteCommand;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeLoteRequest(
        UUID id,
        @NotNull UUID loteId,
        LocalDate fecha,
        @NotNull @Positive BigDecimal pesoKg,
        @Size(max = 200) String dispositivo,
        UUID clienteUuid,
        @Size(max = 200) String idempotencyKey,
        @Size(max = 1000) String observaciones) {

    PesajeLoteCommand command() {
        return new PesajeLoteCommand(id, loteId, fecha, pesoKg, dispositivo, clienteUuid, idempotencyKey, observaciones);
    }
}
