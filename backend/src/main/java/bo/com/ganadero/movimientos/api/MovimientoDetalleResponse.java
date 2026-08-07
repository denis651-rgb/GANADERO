package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.domain.MovimientoDetalle;

import java.util.UUID;

public record MovimientoDetalleResponse(
        UUID id,
        UUID animalId,
        long animalVersionEsperada,
        String estadoAntes,
        String estadoDespues,
        UUID propiedadAntes,
        UUID potreroAntes,
        UUID loteAntes,
        UUID propiedadDespues,
        UUID potreroDespues,
        UUID loteDespues,
        String estadoResultado,
        String mensajeResultado) {

    public static MovimientoDetalleResponse from(MovimientoDetalle detalle) {
        return new MovimientoDetalleResponse(detalle.id(), detalle.animalId(), detalle.animalVersionEsperada(),
                detalle.estadoAntes() == null ? null : detalle.estadoAntes().name(),
                detalle.estadoDespues() == null ? null : detalle.estadoDespues().name(),
                detalle.propiedadAntes(), detalle.potreroAntes(), detalle.loteAntes(),
                detalle.propiedadDespues(), detalle.potreroDespues(), detalle.loteDespues(),
                detalle.estadoResultado(), detalle.mensajeResultado());
    }
}
