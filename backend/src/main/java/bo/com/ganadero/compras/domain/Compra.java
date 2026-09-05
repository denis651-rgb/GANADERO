package bo.com.ganadero.compras.domain;

import bo.com.ganadero.animales.domain.PropositoAnimal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Compra(UUID id, String codigo, UUID proveedorId, Instant fechaRecepcion, ModalidadPrecio modalidad,
                     String moneda, int cantidadAnimales, BigDecimal precioUnitario, BigDecimal precioTotal,
                     BigDecimal precioUnitarioReferencial, UUID propiedadId, UUID potreroId, UUID loteGanaderoId,
                     PropositoAnimal proposito, String observaciones, EstadoCompra estado, String motivoAnulacion,
                     UUID anuladoPor, Instant fechaAnulacion, String origenMigracion, Instant createdAt,
                     UUID createdBy, Instant updatedAt, UUID updatedBy, long version) {
}
