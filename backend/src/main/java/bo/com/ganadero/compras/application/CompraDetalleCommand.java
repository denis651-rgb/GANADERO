package bo.com.ganadero.compras.application;

import bo.com.ganadero.animales.domain.FuenteEdadDeclarada;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.animales.domain.UnidadEdadDeclarada;
import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Datos de una línea de compra (un animal). `precioOverride` null = usa el precio del encabezado
 * (unitario, o distribución uniforme en POR_TROPA); no-null = precio explícito para ese animal. */
public record CompraDetalleCommand(String codigoSolicitado, String nombre, SexoAnimal sexo, UUID razaId,
                                   PropositoAnimal proposito, LocalDate fechaNacimiento, Boolean fechaNacimientoEstimada,
                                   Integer edadDeclaradaValor, UnidadEdadDeclarada edadDeclaradaUnidad,
                                   LocalDate fechaReferenciaEdad, FuenteEdadDeclarada fuenteEdadDeclarada,
                                   String observacionEstimacion, UUID categoriaActualId, String categoriaManualMotivo,
                                   BigDecimal precioOverride, BigDecimal pesoIngresoKg, TipoPeso tipoPeso,
                                   String metodoPeso, UUID propiedadId, UUID potreroId, UUID loteGanaderoId,
                                   String observaciones) {
}
