package bo.com.ganadero.integraciones.calendario;

import java.time.Instant;
import java.util.UUID;

public record ConfiguracionCalendarioExterno(
        UUID id, UUID empresaId, String proveedor, String cuentaEmail, String calendarioExternoId,
        String calendarioNombre, String zonaHoraria, boolean sincronizacionAutomatica,
        EstadoConexionCalendario estado, String ultimoError, Instant ultimaSincronizacion, long version) {
}

