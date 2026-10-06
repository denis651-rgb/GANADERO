package bo.com.ganadero.integraciones.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramIntegrationServiceTest {

    private TelegramConfiguracionRepository configRepository;
    private OutboxAlertaRepository outboxRepository;
    private FirebaseRelaySyncService firebaseSyncService;
    private TelegramIntegrationService service;

    private final UUID empresaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        configRepository = mock(TelegramConfiguracionRepository.class);
        outboxRepository = mock(OutboxAlertaRepository.class);
        firebaseSyncService = mock(FirebaseRelaySyncService.class);
        service = new TelegramIntegrationService(configRepository, outboxRepository, firebaseSyncService);
    }

    @Test
    void consultar_cuandoNoExiste_retornaConfiguracionPorDefecto() {
        when(configRepository.buscarPorEmpresa(empresaId)).thenReturn(Optional.empty());

        ConfiguracionTelegram config = service.consultar(empresaId);

        assertNotNull(config);
        assertEquals(empresaId, config.empresaId());
        assertTrue(config.activo());
        assertTrue(config.notificarSanidad());
        assertTrue(config.notificarReproduccion());
        assertEquals("07:00", config.horaNotificacion());
    }

    @Test
    void guardar_actualizaConfiguracionYSincronizaConFirebase() {
        ConfiguracionTelegram actual = ConfiguracionTelegram.porDefecto(empresaId);
        when(configRepository.buscarPorEmpresa(empresaId)).thenReturn(Optional.of(actual));

        TelegramIntegrationService.GuardarTelegramConfigRequest req =
                new TelegramIntegrationService.GuardarTelegramConfigRequest(
                        "123456:ABC-DEF",
                        "987654321",
                        "[]",
                        true,
                        true,
                        true,
                        "06:30"
                );

        ConfiguracionTelegram guardada = service.guardar(empresaId, req);

        assertEquals("123456:ABC-DEF", guardada.botToken());
        assertEquals("987654321", guardada.chatIdPrincipal());
        assertEquals("06:30", guardada.horaNotificacion());
        verify(configRepository, times(1)).guardar(any(ConfiguracionTelegram.class));
        verify(firebaseSyncService, times(1)).sincronizarConfiguracionTelegram(any(ConfiguracionTelegram.class));
    }

    @Test
    void consultarEstado_retornaValoresEsperados() {
        ConfiguracionTelegram config = new ConfiguracionTelegram(
                UUID.randomUUID(), empresaId, "TOKEN", "CHAT1", "[]",
                true, true, true, "07:00", Instant.now(), Instant.now(), 0
        );
        when(configRepository.buscarPorEmpresa(empresaId)).thenReturn(Optional.of(config));
        when(outboxRepository.contarPendientes(empresaId)).thenReturn(3);
        when(firebaseSyncService.isConectado()).thenReturn(true);

        TelegramIntegrationService.EstadoIntegracionTelegram estado = service.consultarEstado(empresaId);

        assertNotNull(estado);
        assertTrue(estado.activo());
        assertTrue(estado.telegramConfigurado());
        assertTrue(estado.firebaseConectado());
        assertEquals(3, estado.alertasPendientesSincronizacion());
        assertEquals("07:00", estado.horaNotificacion());
    }
}
