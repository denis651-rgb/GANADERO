package bo.com.ganadero.sanidad.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AplicacionTratamiento(UUID id, UUID empresaId, UUID tratamientoDetalleId, Instant fechaProgramada,
                                    Instant fechaAplicada, BigDecimal dosisProgramada, BigDecimal dosisAplicada,
                                    UUID aplicadoPor, EstadoAplicacionTratamiento estado, String observaciones,
                                    long version, LocalDate retiroCarneHasta, LocalDate retiroLecheHasta,
                                    String productoTexto) {

    /** Constructor legado (sin retiro): usado donde todavía no aplica calcular restricción. */
    public AplicacionTratamiento(UUID id, UUID empresaId, UUID tratamientoDetalleId, Instant fechaProgramada,
                                 Instant fechaAplicada, BigDecimal dosisProgramada, BigDecimal dosisAplicada,
                                 UUID aplicadoPor, EstadoAplicacionTratamiento estado, String observaciones,
                                 long version) {
        this(id, empresaId, tratamientoDetalleId, fechaProgramada, fechaAplicada, dosisProgramada, dosisAplicada,
                aplicadoPor, estado, observaciones, version, null, null, null);
    }
}
