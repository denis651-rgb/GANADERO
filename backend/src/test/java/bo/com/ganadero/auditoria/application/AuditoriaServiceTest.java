package bo.com.ganadero.auditoria.application;

import bo.com.ganadero.auditoria.domain.AuditPage;
import bo.com.ganadero.auditoria.domain.AuditoriaFilter;
import bo.com.ganadero.auditoria.domain.AuditoriaRegistro;
import bo.com.ganadero.auditoria.domain.AuditoriaRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditoriaServiceTest {

    private final AuditoriaRepository repository = mock(AuditoriaRepository.class);

    private AuditoriaService service(CurrentUser user) {
        return new AuditoriaService(repository, new UserContext(() -> user));
    }

    private CurrentUser usuario(UUID empresa, String permiso) {
        return new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(),
                Set.of("PROPIETARIO"), Set.of(permiso), Set.of(), true);
    }

    private AuditoriaRegistro registro(UUID empresa) {
        return new AuditoriaRegistro(UUID.randomUUID(), empresa, UUID.randomUUID(), "CREAR",
                "ANIMALES", "ANIMAL", UUID.randomUUID(), "corr-1", "EXITO",
                Map.of(), Map.of(), Map.of("codigo", "T-1"), "WEB", "127.0.0.1",
                "test", Instant.now());
    }

    @Test
    void usuarioConPermisoListaLosRegistrosDeSuEmpresa() {
        UUID empresa = UUID.randomUUID();
        AuditoriaFilter filter = new AuditoriaFilter(null, "ANIMALES", "CREAR", null, null, null,
                null, null, 0, 15);
        AuditPage page = AuditPage.of(List.of(registro(empresa)), 0, 15, 1);
        when(repository.findAll(empresa, filter)).thenReturn(page);

        AuditPage result = service(usuario(empresa, "AUDITORIA_VER")).list(filter);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(repository).findAll(empresa, filter);
    }

    @Test
    void usuarioSinPermisoNoAccedeALaAuditoria() {
        UUID empresa = UUID.randomUUID();
        AuditoriaFilter filter = new AuditoriaFilter(null, null, null, null, null, null,
                null, null, 0, 15);

        assertThatThrownBy(() -> service(usuario(empresa, "ANIMAL_VER")).list(filter))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permiso");

        verify(repository, never()).findAll(any(), any());
    }

    @Test
    void pasaLosFiltrosAvanzadosAlRepositorio() {
        UUID empresa = UUID.randomUUID();
        UUID propiedad = UUID.randomUUID();
        LocalDateTime desde = LocalDateTime.now().minusDays(7);
        LocalDateTime hasta = LocalDateTime.now();
        AuditoriaFilter filter = new AuditoriaFilter(UUID.randomUUID(), "EMPRESAS", "ACTUALIZAR",
                "CONFIGURACION_EMPRESA", propiedad, "corr-42", desde, hasta, 2, 25);
        when(repository.findAll(eq(empresa), any(AuditoriaFilter.class)))
                .thenReturn(AuditPage.of(List.of(), 2, 25, 0));

        service(usuario(empresa, "AUDITORIA_VER")).list(filter);

        verify(repository).findAll(empresa, filter);
    }
}
