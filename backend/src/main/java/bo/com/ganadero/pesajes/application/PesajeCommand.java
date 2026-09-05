package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.pesajes.domain.TipoPesaje;
import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeCommand(
        UUID id,
        UUID animalId,
        LocalDate fecha,
        BigDecimal pesoKg,
        TipoPesaje tipo,
        TipoPeso tipoPeso,
        BigDecimal condicionCorporal,
        String bascula,
        UUID responsableId,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        String dispositivo,
        UUID compraId,
        UUID ventaId,
        UUID movimientoId,
        UUID clienteUuid,
        String idempotencyKey,
        String observaciones) {

    /** Compatibilidad con el shape anterior (sin tipoPeso ni enlaces a compra/venta/movimiento). */
    public PesajeCommand(UUID id, UUID animalId, LocalDate fecha, BigDecimal pesoKg, TipoPesaje tipo,
                         BigDecimal condicionCorporal, String bascula, UUID responsableId, UUID propiedadId,
                         UUID potreroId, UUID loteId, String dispositivo, UUID clienteUuid, String idempotencyKey,
                         String observaciones) {
        this(id, animalId, fecha, pesoKg, tipo, null, condicionCorporal, bascula, responsableId, propiedadId,
                potreroId, loteId, dispositivo, null, null, null, clienteUuid, idempotencyKey, observaciones);
    }
}
