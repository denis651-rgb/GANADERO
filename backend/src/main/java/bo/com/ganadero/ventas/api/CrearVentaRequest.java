package bo.com.ganadero.ventas.api;

import bo.com.ganadero.ventas.application.VentaCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CrearVentaRequest(
        @NotNull UUID animalId,
        LocalDate fechaVenta,
        @NotBlank String comprador,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precio,
        String moneda,
        BigDecimal pesoVentaKg,
        String observaciones) {

    public VentaCommand toCommand() {
        return new VentaCommand(animalId, fechaVenta, comprador, precio, moneda, pesoVentaKg, observaciones);
    }
}
