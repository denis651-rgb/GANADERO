package bo.com.ganadero.alertas.application;

import java.util.UUID;

public record AlertaCanceladaEvent(UUID empresaId, String origenTipo, UUID origenId) {}
