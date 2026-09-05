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
        EstadoPesaje estado,
        String motivoAnulacion,
        UUID anuladoPor,
        Instant fechaAnulacion,
        String observaciones,
        String codigoAnimal,
        String nombreAnimal,
        String loteNombre,
        String potreroNombre,
        String propiedadNombre,
        String responsableNombre,
        long version) {

    /** Compatibilidad con el shape anterior (sin tipoPeso ni enlaces a compra/venta/movimiento). */
    public Pesaje(UUID id, UUID empresaId, UUID animalId, LocalDate fecha, BigDecimal pesoKg, TipoPesaje tipo,
                 BigDecimal condicionCorporal, String bascula, UUID responsableId, UUID propiedadId,
                 UUID potreroId, UUID loteId, String dispositivo, UUID clienteUuid, String idempotencyKey,
                 EstadoPesaje estado, String motivoAnulacion, UUID anuladoPor, Instant fechaAnulacion,
                 String observaciones, String codigoAnimal, String nombreAnimal, String loteNombre,
                 String potreroNombre, String propiedadNombre, String responsableNombre, long version) {
        this(id, empresaId, animalId, fecha, pesoKg, tipo, null, condicionCorporal, bascula, responsableId,
                propiedadId, potreroId, loteId, dispositivo, null, null, null, clienteUuid, idempotencyKey,
                estado, motivoAnulacion, anuladoPor, fechaAnulacion, observaciones, codigoAnimal, nombreAnimal,
                loteNombre, potreroNombre, propiedadNombre, responsableNombre, version);
    }
}
