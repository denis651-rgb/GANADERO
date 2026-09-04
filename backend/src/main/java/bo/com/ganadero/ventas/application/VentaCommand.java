package bo.com.ganadero.ventas.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VentaCommand(UUID animalId, LocalDate fechaVenta, String comprador, BigDecimal precio,
                           String moneda, BigDecimal pesoVentaKg, String observaciones) {
}
