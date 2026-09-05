package bo.com.ganadero.compras.api;

import bo.com.ganadero.compras.domain.CompraDetalle;

import java.math.BigDecimal;
import java.util.UUID;

public record CompraDetalleResponse(UUID id, UUID animalId, int numeroLinea, BigDecimal precioAsignado,
                                    BigDecimal pesoIngresoKg, String tipoPeso, String metodoPeso,
                                    String codigoSolicitado, String nombre, String observaciones) {
    public static CompraDetalleResponse from(CompraDetalle d) {
        return new CompraDetalleResponse(d.id(), d.animalId(), d.numeroLinea(), d.precioAsignado(),
                d.pesoIngresoKg(), d.tipoPeso() == null ? null : d.tipoPeso().name(), d.metodoPeso(),
                d.codigoSolicitado(), d.nombre(), d.observaciones());
    }
}
