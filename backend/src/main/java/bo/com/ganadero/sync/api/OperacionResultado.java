package bo.com.ganadero.sync.api;

import java.util.List;
import java.util.UUID;

public record OperacionResultado(
        UUID clienteId,
        String estado,
        UUID entidadId,
        Long versionServidor,
        Object datosServidor,
        String errorCode,
        String errorMessage,
        List<String> conflictos) {
}
