package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.time.LocalDate;
import java.util.UUID;

public record PesajeLoteCommand(
        UUID id,
        UUID loteId,
        LocalDate fecha,
        java.math.BigDecimal pesoKg,
        TipoPeso tipoPeso,
        String dispositivo,
        UUID clienteUuid,
        String idempotencyKey,
        String observaciones) {

    /** Compatibilidad con el shape anterior (sin tipoPeso: se asume MEDIDO). */
    public PesajeLoteCommand(UUID id, UUID loteId, LocalDate fecha, java.math.BigDecimal pesoKg, String dispositivo,
                             UUID clienteUuid, String idempotencyKey, String observaciones) {
        this(id, loteId, fecha, pesoKg, TipoPeso.MEDIDO, dispositivo, clienteUuid, idempotencyKey, observaciones);
    }
}
