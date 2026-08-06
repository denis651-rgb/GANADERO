package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.domain.MovimientoDetalle;

import java.util.UUID;

public record MovimientoDetalleResponse(UUID id, UUID animalId, String estadoAntes, String estadoDespues) {
    public static MovimientoDetalleResponse from(MovimientoDetalle detalle) {
        return new MovimientoDetalleResponse(detalle.id(), detalle.animalId(),
                detalle.estadoAntes() == null ? null : detalle.estadoAntes().name(),
                detalle.estadoDespues() == null ? null : detalle.estadoDespues().name());
    }
}
