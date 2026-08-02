package bo.com.ganadero.empresas.domain;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmpresaTest {
    private final UUID empresaId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @Test
    void updatesOnlyEditableCompanyData() {
        Empresa empresa = company(3);
        empresa.update("Nueva Razón", "Nuevo Nombre", null, "70000000", null, null, 3, actorId);

        assertThat(empresa.id()).isEqualTo(empresaId);
        assertThat(empresa.codigo()).isEqualTo("EMP-001");
        assertThat(empresa.razonSocial()).isEqualTo("Nueva Razón");
        assertThat(empresa.updatedBy()).isEqualTo(actorId);
    }

    @Test
    void rejectsStaleVersion() {
        assertThatThrownBy(() -> company(4).update("Nueva Razón", null, null, null, null, null, 3, actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    private Empresa company(long version) {
        return new Empresa(empresaId, "EMP-001", "Razón", "Nombre", null, null, null, null,
                "America/La_Paz", "BOB", EstadoEmpresa.ACTIVA, null, Instant.now(), actorId,
                Instant.now(), actorId, version);
    }
}
