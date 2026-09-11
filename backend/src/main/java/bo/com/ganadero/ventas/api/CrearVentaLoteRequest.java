package bo.com.ganadero.ventas.api;

import bo.com.ganadero.ventas.application.VentaLoteCommand;
import bo.com.ganadero.ventas.domain.ModalidadVenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CrearVentaLoteRequest(
        @NotEmpty List<UUID> animalIds,
        LocalDate fechaVenta,
        @NotBlank String comprador,
        String telefonoComprador,
        @NotNull ModalidadVenta modalidad,
        BigDecimal precioCabeza,
        BigDecimal precioKg,
        Map<UUID, BigDecimal> pesosVentaKg,
        String observaciones) {

    public VentaLoteCommand toCommand() {
        return new VentaLoteCommand(animalIds, fechaVenta, comprador, telefonoComprador, modalidad,
                precioCabeza, precioKg, pesosVentaKg, observaciones);
    }
}
