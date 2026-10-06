package bo.com.ganadero.alertas.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AlertaProgramadaEvent(
    UUID id,
    UUID empresaId,
    UUID animalId,
    TipoAlerta tipo,
    String titulo,
    String mensaje,
    String severidad,
    Instant fechaProgramada,
    String origenTipo,
    UUID origenId,
    Map<String, Object> metadata
) {}
