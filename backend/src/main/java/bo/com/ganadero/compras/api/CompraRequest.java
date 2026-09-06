package bo.com.ganadero.compras.api;

import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.compras.application.CompraCommand;
import bo.com.ganadero.compras.domain.ModalidadPrecio;
import bo.com.ganadero.proveedores.api.ProveedorRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Uno de proveedorId (existente) o proveedorNuevo (buscar por documento o crear) debe venir informado. */
public record CompraRequest(UUID proveedorId, @Valid ProveedorRequest proveedorNuevo, @NotNull Instant fechaRecepcion,
                            @NotNull ModalidadPrecio modalidad, @NotNull @Size(max = 10) String moneda,
                            @PositiveOrZero BigDecimal precioUnitario, @PositiveOrZero BigDecimal precioTotal,
                            @NotNull UUID propiedadId, @NotNull UUID potreroId, UUID loteGanaderoId,
                            PropositoAnimal proposito, @Size(max = 1000) String observaciones,
                            @NotEmpty(message = "La compra debe incluir al menos un animal") @Size(max = 500)
                            List<@Valid CompraDetalleRequest> detalles) {

    public CompraCommand command() {
        return new CompraCommand(proveedorId, proveedorNuevo == null ? null : proveedorNuevo.command(),
                fechaRecepcion, modalidad, moneda, precioUnitario, precioTotal, propiedadId, potreroId,
                loteGanaderoId, proposito, observaciones, detalles.stream().map(CompraDetalleRequest::command).toList());
    }
}
