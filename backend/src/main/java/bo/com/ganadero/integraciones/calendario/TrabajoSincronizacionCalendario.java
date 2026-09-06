package bo.com.ganadero.integraciones.calendario;

import java.time.Instant;
import java.util.UUID;

public record TrabajoSincronizacionCalendario(
        UUID id, UUID ocurrenciaId, OperacionCalendario operacion, String claveIdempotencia,
        String payload, EstadoColaCalendario estado, int intentos, int maxIntentos,
        Instant proximoIntento, Instant bloqueadoHasta, String ultimoError, long version) {
}

