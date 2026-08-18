package bo.com.ganadero.alertas.domain;

import java.time.Instant;
import java.util.UUID;

public record Recordatorio(UUID id, UUID empresaId, UUID creadoPor, String titulo, String mensaje,
        SeveridadAlerta severidad, UUID animalId, Instant fechaEvento, Instant proximaEjecucion,
        int cantidadNotificaciones, Integer intervaloMinutos, int notificacionesGeneradas,
        EstadoRecordatorio estado, Instant createdAt, Instant updatedAt, long version) {}
