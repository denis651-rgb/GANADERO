package bo.com.ganadero.compras.api;

import bo.com.ganadero.compras.application.CompraService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResumenCompraAnimalResponse(UUID id, String codigo, Instant fechaRecepcion, String modalidad, String moneda,
                                          BigDecimal precioAsignado, String proveedorNombre, String proveedorTelefono,
                                          String proveedorDocumento) {
    public static ResumenCompraAnimalResponse from(CompraService.ResumenCompraAnimal r) {
        return new ResumenCompraAnimalResponse(r.id(), r.codigo(), r.fechaRecepcion(), r.modalidad(), r.moneda(),
                r.precioAsignado(), r.proveedorNombre(), r.proveedorTelefono(), r.proveedorDocumento());
    }
}
