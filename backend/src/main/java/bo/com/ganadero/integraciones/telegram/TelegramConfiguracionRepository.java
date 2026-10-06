package bo.com.ganadero.integraciones.telegram;

import java.util.Optional;
import java.util.UUID;

public interface TelegramConfiguracionRepository {
    Optional<ConfiguracionTelegram> buscarPorEmpresa(UUID empresaId);
    void guardar(ConfiguracionTelegram config);
}
