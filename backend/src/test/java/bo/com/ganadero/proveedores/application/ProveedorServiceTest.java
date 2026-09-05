package bo.com.ganadero.proveedores.application;

import bo.com.ganadero.proveedores.domain.Proveedor;
import bo.com.ganadero.proveedores.domain.ProveedorRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProveedorServiceTest {
    private ProveedorRepository repo;
    private ProveedorService service;
    private CurrentUser actor;

    @BeforeEach
    void setUp() {
        repo = mock(ProveedorRepository.class);
        UserContext context = new UserContext(() -> actor);
        service = new ProveedorService(repo, context, mock(ApplicationEventPublisher.class));
        actor = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                Set.of("PROVEEDOR_VER", "PROVEEDOR_CREAR", "PROVEEDOR_EDITAR"), Set.of(), true);
        when(repo.crear(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void buscarOCrearReutilizaPorDocumentoExistente() {
        Proveedor existente = new Proveedor(UUID.randomUUID(), "Estancia El Roble", "76543210", "1234567", null,
                null, null, true, Instant.now(), actor.userId(), Instant.now(), actor.userId(), 0);
        when(repo.findByDocumento("1234567")).thenReturn(Optional.of(existente));

        Proveedor resultado = service.buscarOCrear(
                new ProveedorCommand(null, "Otro nombre escrito distinto", null, "1234567", null, null, null, null), actor);

        assertThat(resultado.id()).isEqualTo(existente.id());
        verify(repo, never()).crear(any(), any());
    }

    @Test
    void buscarOCrearCreaUnoNuevoCuandoNoHayDocumentoCoincidente() {
        when(repo.findByDocumento("999")).thenReturn(Optional.empty());

        Proveedor resultado = service.buscarOCrear(
                new ProveedorCommand(null, "Proveedor Nuevo", "70000000", "999", null, null, null, null), actor);

        assertThat(resultado.nombre()).isEqualTo("Proveedor Nuevo");
        verify(repo).crear(any(), eq(actor.userId()));
    }

    @Test
    void buscarOCrearConIdExistenteLoValidaActivo() {
        Proveedor existente = new Proveedor(UUID.randomUUID(), "Estancia El Roble", null, null, null, null, null,
                true, Instant.now(), actor.userId(), Instant.now(), actor.userId(), 0);
        when(repo.findById(existente.id())).thenReturn(Optional.of(existente));

        Proveedor resultado = service.buscarOCrear(
                new ProveedorCommand(existente.id(), null, null, null, null, null, null, null), actor);

        assertThat(resultado).isEqualTo(existente);
    }

    @Test
    void rechazaProveedorIdDeUnoInactivo() {
        Proveedor inactivo = new Proveedor(UUID.randomUUID(), "Viejo Proveedor", null, null, null, null, null,
                false, Instant.now(), actor.userId(), Instant.now(), actor.userId(), 0);
        when(repo.findById(inactivo.id())).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> service.buscarOCrear(
                new ProveedorCommand(inactivo.id(), null, null, null, null, null, null, null), actor))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rechazaCreacionSinNombre() {
        assertThatThrownBy(() -> service.crear(new ProveedorCommand(null, "  ", null, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.PROVEEDOR_DATOS_REQUERIDOS));
    }
}
