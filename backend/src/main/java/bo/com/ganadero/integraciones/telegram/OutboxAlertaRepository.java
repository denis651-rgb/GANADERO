package bo.com.ganadero.integraciones.telegram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxAlertaRepository {
    void encolar(OutboxAlerta alerta);
    List<OutboxAlerta> buscarPendientes(int limite);
    Optional<OutboxAlerta> buscarPorId(UUID id);
    void marcarSincronizado(UUID id);
    void registrarFallo(UUID id, String error, int nuevosIntentos);
    void cancelarPorOrigen(UUID empresaId, String origenTipo, UUID origenId);
    void marcarCancelado(UUID id);
    int contarPendientes(UUID empresaId);
}
