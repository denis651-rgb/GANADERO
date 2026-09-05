package bo.com.ganadero.compras.api;

import bo.com.ganadero.animales.domain.FuenteEdadDeclarada;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.animales.domain.UnidadEdadDeclarada;
import bo.com.ganadero.compras.application.CompraDetalleCommand;
import bo.com.ganadero.pesajes.domain.TipoPeso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompraDetalleRequest(@Size(max = 60) String codigoSolicitado, @Size(max = 160) String nombre,
                                   @NotNull SexoAnimal sexo, UUID razaId, PropositoAnimal proposito,
                                   LocalDate fechaNacimiento, Boolean fechaNacimientoEstimada,
                                   Integer edadDeclaradaValor, UnidadEdadDeclarada edadDeclaradaUnidad,
                                   LocalDate fechaReferenciaEdad, FuenteEdadDeclarada fuenteEdadDeclarada,
                                   String observacionEstimacion, UUID categoriaActualId,
                                   String categoriaManualMotivo, @PositiveOrZero BigDecimal precioOverride,
                                   BigDecimal pesoIngresoKg, TipoPeso tipoPeso, @Size(max = 60) String metodoPeso,
                                   UUID propiedadId, UUID potreroId, UUID loteGanaderoId,
                                   @Size(max = 500) String observaciones) {
    public CompraDetalleCommand command() {
        return new CompraDetalleCommand(codigoSolicitado, nombre, sexo, razaId, proposito, fechaNacimiento,
                fechaNacimientoEstimada, edadDeclaradaValor, edadDeclaradaUnidad, fechaReferenciaEdad,
                fuenteEdadDeclarada, observacionEstimacion, categoriaActualId, categoriaManualMotivo,
                precioOverride, pesoIngresoKg, tipoPeso, metodoPeso, propiedadId, potreroId, loteGanaderoId,
                observaciones);
    }
}
