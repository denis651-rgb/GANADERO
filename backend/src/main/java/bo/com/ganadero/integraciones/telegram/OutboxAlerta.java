package bo.com.ganadero.integraciones.telegram;

import java.time.Instant;
import java.util.UUID;

public record OutboxAlerta(
    UUID id,
    UUID empresaId,
    String tipoModulo,
    String subtipo,
    UUID animalId,
    String animalCodigo,
    String titulo,
    String mensaje,
    Instant fechaProgramada,
    String prioridad,
    String estadoSincronizacion,
    int intentos,
    String ultimoError,
    String origenTipo,
    UUID origenId,
    Instant creadoEn,
    Instant sincronizadoEn
) {
    public static final String ESTADO_PENDIENTE_SUBIDA = "PENDIENTE_SUBIDA";
    public static final String ESTADO_SINCRONIZADO = "SINCRONIZADO";
    public static final String ESTADO_PENDIENTE_CANCELACION = "PENDIENTE_CANCELACION";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    public static final String ESTADO_ERROR_DEFINITIVO = "ERROR_DEFINITIVO";
}
