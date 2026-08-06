package bo.com.ganadero.empresas.infrastructure;

import bo.com.ganadero.empresas.domain.Empresa;
import bo.com.ganadero.empresas.domain.EmpresaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class EmpresaRepositoryAdapter implements EmpresaRepository {
    private final EntityManager entityManager;

    EmpresaRepositoryAdapter(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override public Optional<Empresa> findById(UUID empresaId) {
        return Optional.ofNullable(entityManager.find(EmpresaJpaEntity.class, empresaId)).map(EmpresaJpaEntity::toDomain);
    }

    @Override public Empresa save(Empresa empresa) {
        EmpresaJpaEntity entity = entityManager.find(EmpresaJpaEntity.class, empresa.id());
        entity.apply(empresa);
        entityManager.flush();
        return entity.toDomain();
    }
}
