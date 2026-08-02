package bo.com.ganadero.empresas.infrastructure;

import bo.com.ganadero.empresas.domain.ConfiguracionEmpresa;
import bo.com.ganadero.empresas.domain.ConfiguracionEmpresaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class ConfiguracionEmpresaRepositoryAdapter implements ConfiguracionEmpresaRepository {
    private final EntityManager entityManager;

    ConfiguracionEmpresaRepositoryAdapter(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override public Optional<ConfiguracionEmpresa> findByEmpresaId(UUID empresaId) {
        return Optional.ofNullable(entityManager.find(ConfiguracionEmpresaJpaEntity.class, empresaId))
                .map(ConfiguracionEmpresaJpaEntity::toDomain);
    }

    @Override public ConfiguracionEmpresa save(ConfiguracionEmpresa configuracion) {
        ConfiguracionEmpresaJpaEntity entity = entityManager.find(ConfiguracionEmpresaJpaEntity.class,
                configuracion.empresaId());
        entity.apply(configuracion);
        entityManager.flush();
        return entity.toDomain();
    }
}
