package bo.com.ganadero.lotes.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Lote(
        UUID id,
        UUID empresaId,
        UUID propiedadId,
        String codigo,
        String nombre,
        String descripcion,
        EstadoLote estado,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        long version,
        Integer cantidadMaxima,
        long cantidadActual,
        UUID potreroActualId) {
    public Lote(UUID id, UUID empresaId, UUID propiedadId, String codigo, String nombre,
                String descripcion, EstadoLote estado, LocalDate fechaApertura, LocalDate fechaCierre, long version) {
        this(id, empresaId, propiedadId, codigo, nombre, descripcion, estado, fechaApertura, fechaCierre, version, null, 0, null);
    }

    /** Compatibilidad con el shape anterior (sin la proyección de ubicación operativa). */
    public Lote(UUID id, UUID empresaId, UUID propiedadId, String codigo, String nombre, String descripcion,
                EstadoLote estado, LocalDate fechaApertura, LocalDate fechaCierre, long version,
                Integer cantidadMaxima, long cantidadActual) {
        this(id, empresaId, propiedadId, codigo, nombre, descripcion, estado, fechaApertura, fechaCierre, version,
                cantidadMaxima, cantidadActual, null);
    }
}
