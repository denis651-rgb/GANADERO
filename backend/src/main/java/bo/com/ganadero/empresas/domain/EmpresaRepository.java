package bo.com.ganadero.empresas.domain;

import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository {
    Optional<Empresa> findById(UUID empresaId);
    Empresa save(Empresa empresa);
}
