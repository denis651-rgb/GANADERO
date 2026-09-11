package bo.com.ganadero.ventas.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Venta(UUID id, UUID animalId, UUID movimientoId, LocalDate fechaVenta, String comprador,
                    BigDecimal precio, String moneda, BigDecimal pesoVentaKg, String observaciones,
                    UUID createdBy, Instant createdAt, long version, String telefonoComprador,
                    ModalidadVenta modalidad, BigDecimal precioUnitario, UUID grupoVentaId) {
}
