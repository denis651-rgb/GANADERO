package bo.com.ganadero.compras.domain;

import bo.com.ganadero.animales.domain.FuenteEdadDeclarada;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.animales.domain.UnidadEdadDeclarada;
import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Línea de una compra: mientras la compra está en BORRADOR, `animalId` es null y estos campos
 * describen el animal a crear al confirmar (mismo vocabulario que
 * CrearAnimalesLoteRequest.AnimalLoteItemRequest, para poder reutilizar AnimalService.create).
 */
public record CompraDetalle(UUID id, UUID compraId, UUID animalId, int numeroLinea, BigDecimal precioAsignado,
                            BigDecimal pesoIngresoKg, TipoPeso tipoPeso, String metodoPeso, UUID propiedadId, UUID potreroId,
                            UUID loteGanaderoId, String codigoSolicitado, String nombre, SexoAnimal sexo,
                            UUID razaId, PropositoAnimal proposito, LocalDate fechaNacimiento, boolean fechaNacimientoEstimada,
                            Integer edadDeclaradaValor, UnidadEdadDeclarada edadDeclaradaUnidad,
                            LocalDate fechaReferenciaEdad, FuenteEdadDeclarada fuenteEdadDeclarada,
                            String observacionEstimacion, UUID categoriaActualId, String categoriaManualMotivo,
                            String observaciones, Instant createdAt) {
}
