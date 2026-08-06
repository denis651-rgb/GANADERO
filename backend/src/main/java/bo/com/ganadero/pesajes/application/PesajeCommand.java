package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.pesajes.domain.TipoPesaje;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeCommand(
        UUID id,
        UUID animalId,
        LocalDate fecha,
        BigDecimal pesoKg,
        TipoPesaje tipo,
        BigDecimal condicionCorporal,
        String bascula,
        UUID responsableId,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        String dispositivo,
        UUID clienteUuid,
        String idempotencyKey,
        String observaciones) {
}
