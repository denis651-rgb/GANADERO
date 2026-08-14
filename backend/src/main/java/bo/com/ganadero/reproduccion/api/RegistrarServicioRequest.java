package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.application.RegistrarServicioCommand;
import bo.com.ganadero.reproduccion.domain.TipoServicio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record RegistrarServicioRequest(
        UUID id,
        @NotNull UUID hembraId,
        UUID celoId,
        @NotNull Instant fechaServicio,
        @NotNull TipoServicio tipoServicio,
        UUID machoId,
        @Size(max = 100) String codigoSemen,
        @Size(max = 160) String proveedorSemen,
        UUID tecnicoId,
        @Size(max = 1000) String observaciones,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        UUID clienteUuid,
        @Size(max = 200) String idempotencyKey) {

    RegistrarServicioCommand command() {
        return new RegistrarServicioCommand(id, hembraId, celoId, fechaServicio, tipoServicio, machoId,
                codigoSemen, proveedorSemen, tecnicoId, observaciones, propiedadId, potreroId, loteId,
                clienteUuid, idempotencyKey);
    }
}
