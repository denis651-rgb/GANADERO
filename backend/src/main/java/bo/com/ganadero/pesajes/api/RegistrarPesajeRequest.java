package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.application.PesajeCommand;
import bo.com.ganadero.pesajes.domain.TipoPesaje;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarPesajeRequest(
        UUID id,
        @NotNull UUID animalId,
        LocalDate fecha,
        @NotNull @Positive BigDecimal pesoKg,
        TipoPesaje tipo,
        @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal condicionCorporal,
        @Size(max = 120) String bascula,
        UUID responsableId,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        @Size(max = 200) String dispositivo,
        UUID clienteUuid,
        @Size(max = 200) String idempotencyKey,
        @Size(max = 1000) String observaciones) {

    PesajeCommand command() {
        return new PesajeCommand(id, animalId, fecha, pesoKg, tipo, condicionCorporal, bascula,
                responsableId, propiedadId, potreroId, loteId, dispositivo, clienteUuid, idempotencyKey, observaciones);
    }
}
