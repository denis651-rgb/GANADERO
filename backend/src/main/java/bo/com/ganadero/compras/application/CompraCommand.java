package bo.com.ganadero.compras.application;

import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.compras.domain.ModalidadPrecio;
import bo.com.ganadero.proveedores.application.ProveedorCommand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Uno de proveedorId (existente) o proveedorNuevo (buscar-o-crear) debe venir informado. */
public record CompraCommand(UUID proveedorId, ProveedorCommand proveedorNuevo, Instant fechaRecepcion,
                            ModalidadPrecio modalidad, String moneda, BigDecimal precioUnitario,
                            BigDecimal precioTotal, UUID propiedadId, UUID potreroId, UUID loteGanaderoId,
                            PropositoAnimal proposito, String observaciones, List<CompraDetalleCommand> detalles) {
}
