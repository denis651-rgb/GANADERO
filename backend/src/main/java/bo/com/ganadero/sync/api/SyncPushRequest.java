package bo.com.ganadero.sync.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncPushRequest(
        @NotNull DispositivoInfo dispositivo,
        @NotNull @Size(max = 2000) List<OperacionRequest> operaciones) {

    public record DispositivoInfo(
            @NotBlank @Size(max = 120) String codigo,
            @Size(max = 160) String nombre,
            String plataforma,
            @Size(max = 60) String versionApp) {
    }

    public record OperacionRequest(
            @NotNull UUID clienteId,
            @NotBlank @Size(max = 40) String tipo,
            @Size(max = 40) String entidad,
            UUID entidadId,
            Long versionCliente,
            @Size(max = 200) String idempotencyKey,
            Map<String, Object> datos) {
    }
}
