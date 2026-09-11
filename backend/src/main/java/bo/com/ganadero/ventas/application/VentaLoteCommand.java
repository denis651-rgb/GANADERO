package bo.com.ganadero.ventas.application;

import bo.com.ganadero.ventas.domain.ModalidadVenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Venta de varios animales (selección manual o lote completo, ya resuelto a IDs por el cliente)
 * en una sola operación. En {@link ModalidadVenta#EN_PIE} todos comparten {@code precioCabeza};
 * en {@link ModalidadVenta#CARNEADO} cada animal se cobra a {@code precioKg} según su propio peso
 * en {@code pesosVentaKg}.
 */
public record VentaLoteCommand(List<UUID> animalIds, LocalDate fechaVenta, String comprador,
                               String telefonoComprador, ModalidadVenta modalidad, BigDecimal precioCabeza,
                               BigDecimal precioKg, Map<UUID, BigDecimal> pesosVentaKg, String observaciones) {
}
