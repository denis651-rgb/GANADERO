package bo.com.ganadero.ventas.application;

import bo.com.ganadero.pesajes.domain.TipoPeso;
import bo.com.ganadero.ventas.domain.ModalidadVenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VentaCommand(UUID animalId, LocalDate fechaVenta, String comprador, BigDecimal precio,
                           String moneda, BigDecimal pesoVentaKg, String observaciones,
                           UUID pesajeExistenteId, TipoPeso tipoPeso, String dispositivo,
                           String telefonoComprador, ModalidadVenta modalidad) {

    /** Compatibilidad con el shape anterior (sin teléfono ni modalidad explícita). */
    public VentaCommand(UUID animalId, LocalDate fechaVenta, String comprador, BigDecimal precio,
                        String moneda, BigDecimal pesoVentaKg, String observaciones,
                        UUID pesajeExistenteId, TipoPeso tipoPeso, String dispositivo) {
        this(animalId, fechaVenta, comprador, precio, moneda, pesoVentaKg, observaciones,
                pesajeExistenteId, tipoPeso, dispositivo, null, null);
    }

    /** Compatibilidad con el shape anterior (sin datos de pesaje estructurados). */
    public VentaCommand(UUID animalId, LocalDate fechaVenta, String comprador, BigDecimal precio,
                        String moneda, BigDecimal pesoVentaKg, String observaciones) {
        this(animalId, fechaVenta, comprador, precio, moneda, pesoVentaKg, observaciones, null, null, null);
    }
}
