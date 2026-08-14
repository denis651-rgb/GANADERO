package bo.com.ganadero.reproduccion.domain;

import java.time.Instant;
import java.util.UUID;

public record Servicio(
        UUID id,
        UUID empresaId,
        UUID hembraId,
        UUID celoId,
        Instant fechaServicio,
        TipoServicio tipoServicio,
        UUID machoId,
        String codigoSemen,
        String proveedorSemen,
        UUID tecnicoId,
        int numeroIntento,
        Instant fechaDiagnosticoRecomendada,
        String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        String idempotencyKey,
        EstadoServicio estado,
        Instant anuladoAt,
        UUID anuladoBy,
        String motivoAnulacion,
        String codigoAnimal,
        String nombreAnimal,
        String codigoMacho,
        String nombreMacho,
        String potreroNombre,
        String propiedadNombre,
        long version) {
    public Servicio(UUID id, UUID empresaId, UUID hembraId, UUID celoId, java.time.LocalDate fechaServicio,
                    TipoServicio tipoServicio, UUID machoId, int numeroIntento, String observaciones,
                    UUID propiedadId, UUID potreroId, UUID loteId, UUID clienteUuid, String idempotencyKey,
                    EstadoRegistroReproduccion estado, String codigoAnimal, String nombreAnimal,
                    String codigoMacho, String nombreMacho, String potreroNombre, String propiedadNombre,
                    long version) {
        this(id, empresaId, hembraId, celoId, fechaServicio.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                tipoServicio, machoId, null, null, null, numeroIntento,
                fechaServicio.plusDays(28).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), observaciones,
                propiedadId, potreroId, loteId, clienteUuid, idempotencyKey,
                estado == EstadoRegistroReproduccion.ANULADO ? EstadoServicio.ANULADO : EstadoServicio.PENDIENTE_DIAGNOSTICO,
                null, null, null, codigoAnimal, nombreAnimal, codigoMacho, nombreMacho, potreroNombre,
                propiedadNombre, version);
    }
}
