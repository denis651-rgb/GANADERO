package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PlanSanitario(UUID id, UUID empresaId, String nombre, String descripcion, LocalDate fechaInicio,
                            LocalDate fechaFin, EstadoPlanSanitario estado, Instant createdAt, Instant updatedAt,
                            long version, UUID propiedadId) {

    /** Constructor legado: alcance global (sin propiedad). */
    public PlanSanitario(UUID id, UUID empresaId, String nombre, String descripcion, LocalDate fechaInicio,
                        LocalDate fechaFin, EstadoPlanSanitario estado, Instant createdAt, Instant updatedAt,
                        long version) {
        this(id, empresaId, nombre, descripcion, fechaInicio, fechaFin, estado, createdAt, updatedAt, version, null);
    }
}
