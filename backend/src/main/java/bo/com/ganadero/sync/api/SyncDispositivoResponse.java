package bo.com.ganadero.sync.api;

import java.util.UUID;

public record SyncDispositivoResponse(
        UUID id,
        String codigoDispositivo,
        String nombre,
        String plataforma,
        String versionApp,
        String estado,
        long ultimoCursor) {
}
