package bo.com.ganadero.empresas.domain;

import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionEmpresaRepository {
    Optional<ConfiguracionEmpresa> findByEmpresaId(UUID empresaId);
    ConfiguracionEmpresa save(ConfiguracionEmpresa configuracion);
}
