package bo.com.ganadero.pesajes.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Pesaje(
        UUID id,
        UUID empresaId,
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
        EstadoPesaje estado,
        String motivoAnulacion,
        UUID anuladoPor,
        Instant fechaAnulacion,
        String observaciones,
        String codigoAnimal,
        String nombreAnimal,
        long version) {
}
