package bo.com.ganadero.empresas.application;

import bo.com.ganadero.empresas.domain.Empresa;
import bo.com.ganadero.empresas.domain.EmpresaRepository;
import bo.com.ganadero.empresas.domain.EstadoEmpresa;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmpresaUseCasesTest {
    @Test
    void queryUsesCompanyFromAuthenticatedContext() {
        UUID companyId = UUID.randomUUID();
        CapturingRepository repository = new CapturingRepository(company(companyId));
        UserContext context = context(companyId, Set.of("EMPRESA_VER"));

        Empresa result = new ConsultarEmpresaUseCase(repository, context).execute();

        assertThat(repository.requestedId).isEqualTo(companyId);
        assertThat(result.id()).isEqualTo(companyId);
    }

    @Test
    void updateRequiresExplicitPermission() {
        UUID companyId = UUID.randomUUID();
        ActualizarEmpresaUseCase useCase = new ActualizarEmpresaUseCase(
                new CapturingRepository(company(companyId)), context(companyId, Set.of()), event -> {});

        assertThatThrownBy(() -> useCase.execute(new ActualizarEmpresaCommand(
                "Nueva", null, null, null, null, null, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.USER_NOT_AUTHORIZED));
    }

    private UserContext context(UUID companyId, Set<String> permissions) {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), companyId, UUID.randomUUID(),
                Set.of("PROPIETARIO"), permissions, Set.of(), true);
        return new UserContext(() -> user);
    }

    private Empresa company(UUID id) {
        return new Empresa(id, "EMP-001", "Razón", "Nombre", null, null, null, null,
                "America/La_Paz", "BOB", EstadoEmpresa.ACTIVA, null, Instant.now(), null,
                Instant.now(), null, 0);
    }

    private static class CapturingRepository implements EmpresaRepository {
        private final Empresa company;
        private UUID requestedId;
        private CapturingRepository(Empresa company) { this.company = company; }
        @Override public Optional<Empresa> findById(UUID empresaId) {
            requestedId = empresaId; return Optional.of(company);
        }
        @Override public Empresa save(Empresa empresa) { return empresa; }
    }
}
