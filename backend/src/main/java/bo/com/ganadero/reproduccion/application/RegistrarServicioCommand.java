package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.reproduccion.domain.TipoServicio;

import java.time.Instant;
import java.util.UUID;

public record RegistrarServicioCommand(
        UUID id,
        UUID hembraId,
        UUID celoId,
        Instant fechaServicio,
        TipoServicio tipoServicio,
        UUID machoId,
        String codigoSemen,
        String proveedorSemen,
        UUID tecnicoId,
        String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        String idempotencyKey) {
    public RegistrarServicioCommand(UUID id, UUID hembraId, UUID celoId, java.time.LocalDate fechaServicio,
                                    TipoServicio tipoServicio, UUID machoId, String observaciones,
                                    UUID propiedadId, UUID potreroId, UUID loteId, UUID clienteUuid,
                                    String idempotencyKey) {
        this(id, hembraId, celoId, fechaServicio.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                tipoServicio, machoId, null, null, null, observaciones, propiedadId, potreroId, loteId,
                clienteUuid, idempotencyKey);
    }
}
