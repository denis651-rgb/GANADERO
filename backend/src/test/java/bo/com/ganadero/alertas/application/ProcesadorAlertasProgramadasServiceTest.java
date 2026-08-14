package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.EntregaRepository;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.alertas.domain.SuscripcionPush;
import bo.com.ganadero.alertas.domain.SuscripcionPushRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ProcesadorAlertasProgramadasServiceTest {

    private AlertaRepository alertas;
    private SuscripcionPushRepository suscripciones;
    private EntregaRepository entregas;
    private ObjectProvider<PushNotificadorPort> provider;
    private PushNotificadorPort push;
    private ProcesadorAlertasProgramadasService service;

    private UUID company;
    private SuscripcionPush sub;
    private Alerta alerta;

    @BeforeEach
    void setup() {
        alertas = mock(AlertaRepository.class);
        suscripciones = mock(SuscripcionPushRepository.class);
        entregas = mock(EntregaRepository.class);
        provider = mock(ObjectProvider.class);
        push = mock(PushNotificadorPort.class);
        when(provider.getIfAvailable()).thenReturn(push);
        service = new ProcesadorAlertasProgramadasService(alertas, suscripciones, entregas, provider, true, 5);

        company = UUID.randomUUID();
        sub = new SuscripcionPush(UUID.randomUUID(), company, UUID.randomUUID(),
                "https://push.example.com/abc", "p256", "auth", "Chrome", "Mozilla", true,
                Instant.now(), Instant.now(), Instant.now());
        alerta = new Alerta(UUID.randomUUID(), company, UUID.randomUUID(), TipoAlerta.PROXIMO_PARTO,
                "Parto próximo", "La vaca está por parir", SeveridadAlerta.URGENTE,
                Instant.now().minusSeconds(60), Instant.now(), "reproduccion", UUID.randomUUID(),
                EstadoAlerta.PENDIENTE, null, null, null, null, null, null, null, null, 0, null,
                Instant.now(), Instant.now());
    }

    private PreferenciasNotificacion preferencias(boolean activa) {
        return new PreferenciasNotificacion(company, sub.usuarioId(), activa, activa, activa, activa, activa,
                activa, activa, activa);
    }

    @Test
    void noEnviaCuandoPushDeshabilitado() {
        ProcesadorAlertasProgramadasService sinPush =
                new ProcesadorAlertasProgramadasService(alertas, suscripciones, entregas, provider, false, 5);
        int enviadas = sinPush.enviarPendientes();
        assertThat(enviadas).isZero();
        verifyNoInteractions(alertas);
        verifyNoInteractions(suscripciones);
    }

    @Test
    void activaVencidasDelegaAlRepositorio() {
        when(alertas.activarVencidas(any(), eq(200))).thenReturn(3);
        assertThat(service.activarVencidas()).isEqualTo(3);
    }

    @Test
    void sinSuscriptoresMarcaComoEnviada() {
        when(alertas.listarPendientesEnvio(any(), eq(5), eq(50))).thenReturn(List.of(alerta));
        when(suscripciones.listarActivas(company)).thenReturn(List.of());

        assertThat(service.enviarPendientes()).isZero();
        verify(alertas).marcarEnviada(alerta.id());
        verifyNoInteractions(entregas);
    }

    @Test
    void sinSuscriptoresInteresadosMarcaComoEnviadaSinEnviar() {
        when(alertas.listarPendientesEnvio(any(), eq(5), eq(50))).thenReturn(List.of(alerta));
        when(suscripciones.listarActivas(company)).thenReturn(List.of(sub));
        when(suscripciones.preferencias(company, sub.usuarioId()))
                .thenReturn(new PreferenciasNotificacion(company, sub.usuarioId(), false, false, false, false,
                        false, false, false, false));

        assertThat(service.enviarPendientes()).isZero();
        verify(push, never()).enviar(any(), any());
        verify(alertas).marcarEnviada(alerta.id());
    }

    @Test
    void enviaYRegistraEntregaExitosa() {
        when(alertas.listarPendientesEnvio(any(), eq(5), eq(50))).thenReturn(List.of(alerta));
        when(suscripciones.listarActivas(company)).thenReturn(List.of(sub));
        when(suscripciones.preferencias(company, sub.usuarioId())).thenReturn(preferencias(true));
        when(push.enviar(alerta, sub)).thenReturn(PushNotificadorPort.ResultadoEnvio.ok());

        assertThat(service.enviarPendientes()).isEqualTo(1);
        verify(entregas).registrarPendiente(alerta.id(), sub.id());
        verify(entregas).marcarEnviada(eq(alerta.id()), eq(sub.id()), any());
        verify(alertas).marcarEnviada(alerta.id());
        verify(alertas, never()).registrarFallo(any(), any(), anyInt());
    }

    @Test
    void suscripcionInvalidaDesactivaYMarcaEnviada() {
        when(alertas.listarPendientesEnvio(any(), eq(5), eq(50))).thenReturn(List.of(alerta));
        when(suscripciones.listarActivas(company)).thenReturn(List.of(sub));
        when(suscripciones.preferencias(company, sub.usuarioId())).thenReturn(preferencias(true));
        when(push.enviar(alerta, sub))
                .thenReturn(PushNotificadorPort.ResultadoEnvio.invalida("HTTP 410"));

        assertThat(service.enviarPendientes()).isZero();
        verify(suscripciones).desactivarTodas(sub.id(), company);
        verify(alertas).marcarEnviada(alerta.id());
    }

    @Test
    void falloTemporalRegistraErrorParaReintento() {
        when(alertas.listarPendientesEnvio(any(), eq(5), eq(50))).thenReturn(List.of(alerta));
        when(suscripciones.listarActivas(company)).thenReturn(List.of(sub));
        when(suscripciones.preferencias(company, sub.usuarioId())).thenReturn(preferencias(true));
        when(push.enviar(alerta, sub))
                .thenReturn(PushNotificadorPort.ResultadoEnvio.fallo("HTTP 500"));

        assertThat(service.enviarPendientes()).isZero();
        verify(entregas).marcarError(alerta.id(), sub.id(), "HTTP 500");
        verify(alertas).registrarFallo(alerta.id(), "HTTP 500", 5);
        verify(alertas, never()).marcarEnviada(alerta.id());
    }

    @Test
    void respetaPreferenciasPorTipoYSeveridad() {
        Alerta tratamiento = new Alerta(UUID.randomUUID(), company, UUID.randomUUID(),
                TipoAlerta.TRATAMIENTO_PENDIENTE, "Tratamiento", "Pendiente", SeveridadAlerta.INFO,
                Instant.now().minusSeconds(60), Instant.now(), "sanidad", UUID.randomUUID(),
                EstadoAlerta.PENDIENTE, null, null, null, null, null, null, null, null, 0, null,
                Instant.now(), Instant.now());
        PreferenciasNotificacion soloReproduccion = new PreferenciasNotificacion(company, sub.usuarioId(),
                true, false, false, false, false, true, true, true);

        when(alertas.listarPendientesEnvio(any(), eq(5), eq(50))).thenReturn(List.of(tratamiento));
        when(suscripciones.listarActivas(company)).thenReturn(List.of(sub));
        when(suscripciones.preferencias(company, sub.usuarioId())).thenReturn(soloReproduccion);

        assertThat(service.enviarPendientes()).isZero();
        verify(push, never()).enviar(any(), any());
        verify(alertas).marcarEnviada(tratamiento.id());
    }
}
