package bo.com.ganadero.sanidad.domain;

import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Ejecución sanitaria individual de una jornada (sección 22): trazabilidad completa de lo recomendado vs. lo aplicado. */
public record AplicacionSanitaria(
        UUID id, UUID empresaId, UUID jornadaId, UUID planItemId, UUID animalId, UUID productoId,
        UUID loteProductoId, BigDecimal dosis, String unidadDosis, BigDecimal dosisRecomendada,
        BigDecimal dosisAplicada, BigDecimal pesoUtilizadoKg, TipoPeso pesoTipo, java.time.Instant pesoFecha,
        String productoAplicadoTexto, String motivoCambioProducto, String motivoAjusteDosis,
        String viaAdministracion, LugarAplicacion lugarAplicacion, UUID versionActividadId,
        String instruccionesAplicadasTexto, UUID eventoCalendarioId, LocalDate fechaAplicacion,
        LocalDate proximaAplicacion, LocalDate retiroCarneHasta, LocalDate retiroLecheHasta, UUID aplicadoPor,
        String resultado, String observaciones, String idempotencyKey, EstadoAplicacionSanitaria estado,
        long version, OrigenRegistroAplicacion origenRegistro) {
}
