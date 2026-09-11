package bo.com.ganadero.reportes.domain;

import bo.com.ganadero.ventas.domain.ModalidadVenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fila aplanada (con código/nombre/raza del animal) para el reporte de ventas de un período. */
public record ReporteVenta(UUID ventaId, UUID animalId, String codigo, String nombre, String raza,
                           LocalDate fechaVenta, String comprador, String telefonoComprador, BigDecimal precio,
                           String moneda, BigDecimal pesoVentaKg, ModalidadVenta modalidad,
                           BigDecimal precioUnitario) {
}
