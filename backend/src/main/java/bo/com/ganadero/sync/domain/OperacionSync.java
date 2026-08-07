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
        EstadoOperacionSync estado,
        String resultadoCodigo,
        String resultadoMensaje,
        String resultadoServidorJson,
        Long versionServidor,
        String conflictosJson,
        String idempotencyKey,
        String payloadHash,
        int attempts,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant appliedAt) {

    public OperacionSync conResultado(EstadoOperacionSync estado, String codigo, String mensaje, String servidor,
                                      Long versionServidor, String conflictos, UUID entidadId) {
        return new OperacionSync(id, empresaId, dispositivoId, usuarioId, clienteId, tipo, entidad, entidadId,
                datosJson, versionCliente, estado, codigo, mensaje, servidor, versionServidor, conflictos,
                idempotencyKey, payloadHash, attempts, nextRetryAt, lastError, createdAt, Instant.now());
    }

    public OperacionSync conReintento(int attempts, Instant nextRetryAt, String lastError) {
        return new OperacionSync(id, empresaId, dispositivoId, usuarioId, clienteId, tipo, entidad, entidadId,
                datosJson, versionCliente, estado, resultadoCodigo, resultadoMensaje, resultadoServidorJson,
                versionServidor, conflictosJson, idempotencyKey, payloadHash, attempts, nextRetryAt, lastError,
                createdAt, appliedAt);
    }
}
