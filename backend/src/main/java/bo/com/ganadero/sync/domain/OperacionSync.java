package bo.com.ganadero.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record OperacionSync(
        UUID id,
        UUID empresaId,
        UUID dispositivoId,
        UUID usuarioId,
        UUID clienteId,
        String tipo,
        String entidad,
        UUID entidadId,
        String datosJson,
        long versionCliente,
        String estado,
        String resultadoCodigo,
        String resultadoMensaje,
        String resultadoServidorJson,
        Long versionServidor,
        String conflictosJson,
        String idempotencyKey,
        Instant createdAt,
        Instant appliedAt) {

    public OperacionSync conResultado(String estado, String codigo, String mensaje, String servidor,
                                      Long versionServidor, String conflictos, UUID entidadId) {
        return new OperacionSync(id, empresaId, dispositivoId, usuarioId, clienteId, tipo, entidad, entidadId,
                datosJson, versionCliente, estado, codigo, mensaje, servidor, versionServidor, conflictos,
                idempotencyKey, createdAt, Instant.now());
    }
}
