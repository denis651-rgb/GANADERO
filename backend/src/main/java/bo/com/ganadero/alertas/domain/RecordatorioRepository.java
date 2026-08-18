package bo.com.ganadero.alertas.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordatorioRepository {
    Recordatorio guardar(Recordatorio recordatorio);
    List<Recordatorio> listar(UUID empresaId);
    Optional<Recordatorio> buscar(UUID id, UUID empresaId);
    List<Recordatorio> bloquearVencidos(Instant ahora, int limite);
    void registrarEjecucion(Recordatorio recordatorio, Instant siguiente, boolean completado);
    Recordatorio cambiarEstado(UUID id, UUID empresaId, EstadoRecordatorio estado, long version);
    void cancelarAlertas(UUID id, UUID empresaId);
}
