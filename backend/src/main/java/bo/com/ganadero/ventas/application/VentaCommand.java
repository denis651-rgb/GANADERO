package bo.com.ganadero.ventas.application;

import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VentaCommand(UUID animalId, LocalDate fechaVenta, String comprador, BigDecimal precio,
                           String moneda, BigDecimal pesoVentaKg, String observaciones,
                           UUID pesajeExistenteId, TipoPeso tipoPeso, String dispositivo) {

    /** Compatibilidad con el shape anterior (sin datos de pesaje estructurados). */
    public VentaCommand(UUID animalId, LocalDate fechaVenta, String comprador, BigDecimal precio,
                        String moneda, BigDecimal pesoVentaKg, String observaciones) {
        this(animalId, fechaVenta, comprador, precio, moneda, pesoVentaKg, observaciones, null, null, null);
    }
}
