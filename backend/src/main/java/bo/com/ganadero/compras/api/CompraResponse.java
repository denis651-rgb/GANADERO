package bo.com.ganadero.compras.api;

import bo.com.ganadero.compras.domain.Compra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompraResponse(UUID id, String codigo, UUID proveedorId, Instant fechaRecepcion, String modalidad,
                             String moneda, int cantidadAnimales, BigDecimal precioUnitario, BigDecimal precioTotal,
                             BigDecimal precioUnitarioReferencial, UUID propiedadId, UUID potreroId,
                             UUID loteGanaderoId, String proposito, String observaciones, String estado,
                             String motivoAnulacion, Instant fechaAnulacion, String origenMigracion, long version) {
    public static CompraResponse from(Compra c) {
        return new CompraResponse(c.id(), c.codigo(), c.proveedorId(), c.fechaRecepcion(),
                c.modalidad() == null ? null : c.modalidad().name(), c.moneda(), c.cantidadAnimales(),
                c.precioUnitario(), c.precioTotal(), c.precioUnitarioReferencial(), c.propiedadId(), c.potreroId(),
                c.loteGanaderoId(), c.proposito() == null ? null : c.proposito().name(), c.observaciones(),
                c.estado() == null ? null : c.estado().name(), c.motivoAnulacion(), c.fechaAnulacion(),
                c.origenMigracion(), c.version());
    }
}
