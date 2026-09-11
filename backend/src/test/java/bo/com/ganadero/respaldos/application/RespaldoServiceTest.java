package bo.com.ganadero.respaldos.application;

import bo.com.ganadero.respaldos.domain.EstadoRespaldo;
import bo.com.ganadero.respaldos.domain.IntegridadRespaldo;
import bo.com.ganadero.respaldos.domain.Respaldo;
import bo.com.ganadero.respaldos.domain.RespaldoRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cubre la validación de nombre/ruta (defensa contra path traversal) y las guardas de eliminación,
 * que no requieren tocar el sistema de archivos ni una base SQLite real. El flujo completo de
 * creación (VACUUM INTO + integrity_check + hash + zip) se prueba en RespaldoServiceIntegrationTest.
 */
class RespaldoServiceTest {
    private RespaldoRepository repo;
    private RespaldoService service;

    @BeforeEach
    void setUp() {
        repo = mock(RespaldoRepository.class);
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                Set.of("RESPALDOS_VER", "RESPALDOS_CREAR", "RESPALDOS_ELIMINAR", "RESPALDOS_RESTAURAR", "RESPALDOS_CONFIGURAR"),
                Set.of(), true);
        service = new RespaldoService(repo, new UserContext(() -> user), new ObjectMapper(), mock(JdbcClient.class),
                "./data/ganadero.db", "", "0.0.1-TEST");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "Ganadero_2026-01-01_00-00-00.ganadero-backup/../secret",
            "Ganadero_2026-01-01_00-00-00.zip",
            "ganadero_2026-01-01_00-00-00.ganadero-backup",
            "Ganadero_2026-1-1_0-0-0.ganadero-backup",
            "..\\..\\ganadero.db",
    })
    void rechazaNombresQueNoCumplenElPatronEstricto(String nombre) {
        assertThatThrownBy(() -> service.verificar(nombre))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_NOMBRE_INVALIDO));
    }

    @Test
    void rechazaEliminarElUnicoRespaldoValido() {
        String nombre = "Ganadero_2026-01-01_00-00-00.ganadero-backup";
        when(repo.buscar(nombre)).thenReturn(Optional.of(respaldo(nombre, EstadoRespaldo.CREADO_LOCALMENTE, IntegridadRespaldo.VALIDA)));
        when(repo.contarValidos()).thenReturn(1L);

        assertThatThrownBy(() -> service.eliminar(nombre))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_ES_EL_UNICO_VALIDO));
    }

    @Test
    void rechazaEliminarUnRespaldoEnProgreso() {
        String nombre = "Ganadero_2026-01-01_00-00-00.ganadero-backup";
        when(repo.buscar(nombre)).thenReturn(Optional.of(respaldo(nombre, EstadoRespaldo.COPIANDO_A_CARPETA_EXTERNA, IntegridadRespaldo.VALIDA)));

        assertThatThrownBy(() -> service.eliminar(nombre))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_OPERACION_EN_CURSO));
    }

    @Test
    void rechazaEliminarUnRespaldoDeIntegridadDesconocida() {
        String nombre = "Ganadero_2026-01-01_00-00-00.ganadero-backup";
        when(repo.buscar(nombre)).thenReturn(Optional.of(respaldo(nombre, EstadoRespaldo.CREADO_LOCALMENTE, IntegridadRespaldo.DESCONOCIDA)));

        assertThatThrownBy(() -> service.eliminar(nombre))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_INTEGRIDAD_INVALIDA));
    }

    @Test
    void rechazaOperarSobreUnRespaldoInexistente() {
        String nombre = "Ganadero_2026-01-01_00-00-00.ganadero-backup";
        when(repo.buscar(nombre)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(nombre))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_NOT_FOUND));
    }

    private Respaldo respaldo(String nombre, EstadoRespaldo estado, IntegridadRespaldo integridad) {
        return new Respaldo(nombre, Instant.now(), 1024, "hash", 1, "0.0.1-TEST", "37", UUID.randomUUID(),
                estado, integridad, null, null, UUID.randomUUID(), Instant.now());
    }
}
