package bo.com.ganadero.seguridad.invitaciones;

import bo.com.ganadero.seguridad.application.SeguridadAuditEvent;
import bo.com.ganadero.seguridad.domain.EstadoMiembro;
import bo.com.ganadero.seguridad.domain.MiembroEmpresa;
import bo.com.ganadero.seguridad.domain.MiembroEmpresaRepository;
import bo.com.ganadero.seguridad.domain.PerfilUsuario;
import bo.com.ganadero.seguridad.domain.PerfilUsuarioRepository;
import bo.com.ganadero.seguridad.domain.UsuarioActual;
import bo.com.ganadero.seguridad.infrastructure.SupabaseAuthAdminClient;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.config.InvitationProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitacionServiceTest {
    private static final String EMAIL = "invitado@example.com";

    @Test
    void crearCreaYEnviaInvitacion() {
        UUID empresaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        CurrentUser actor = actor(actorId, empresaId, "USUARIO_CREAR");

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findActiveByEmpresaAndEmail(eq(empresaId), eq(EMAIL))).thenReturn(Optional.empty());
        when(invitations.insert(eq(empresaId), eq(EMAIL), eq(actorId), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 0, null, null, 0));
        when(invitations.markEnviada(eq(invId), eq(empresaId), eq(0L), eq(userId), eq(miembroId), any(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 1, userId, miembroId, 1));

        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        when(members.existsActiveByEmail(eq(empresaId), eq(EMAIL))).thenReturn(false);
        when(members.create(eq(empresaId), eq(userId), isNull(), eq(false), eq(actorId)))
                .thenReturn(miembro(miembroId, empresaId, userId, EstadoMiembro.ACTIVO));

        SupabaseAuthAdminClient auth = mock(SupabaseAuthAdminClient.class);
        when(auth.invite(eq(EMAIL), anyString()))
                .thenReturn(new SupabaseAuthAdminClient.AdminUser(userId, true));
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        ejecutarTransaccion(transactions);
        JdbcClient jdbc = fluentJdbc();
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                auth, jdbc, transactions, events, actor);

        InvitacionUsuario result = service.crear(EMAIL, null);

        assertThat(result.usuarioId()).isEqualTo(userId);
        assertThat(result.intentosEnvio()).isEqualTo(1);
        verify(invitations).insert(eq(empresaId), eq(EMAIL), eq(actorId), any());
        verify(invitations).markEnviada(eq(invId), eq(empresaId), eq(0L), eq(userId), eq(miembroId), any(), any());
        verify(members).changeStatus(eq(miembroId), eq(empresaId), eq(EstadoMiembro.INVITADO), eq(0L), eq(actorId));
        verify(events).publishEvent(any(SeguridadAuditEvent.class));
    }

    @Test
    void crearRechazaCuandoElEmailYaEsMiembro() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_CREAR");
        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        when(members.existsActiveByEmail(any(), eq(EMAIL))).thenReturn(true);
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                mock(SupabaseAuthAdminClient.class), fluentJdbc(), mock(TransactionTemplate.class),
                mock(ApplicationEventPublisher.class), actor);

        assertCode(() -> service.crear(EMAIL, null), ErrorCode.USER_ALREADY_MEMBER);
        verify(invitations, never()).insert(any(), any(), any(), any());
    }

    @Test
    void crearRechazaCuandoYaExisteInvitacionActiva() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_CREAR");
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findActiveByEmpresaAndEmail(any(), eq(EMAIL)))
                .thenReturn(Optional.of(invitacion(UUID.randomUUID(), actor.empresaId(),
                        EstadoInvitacion.PENDIENTE, future(), 1, UUID.randomUUID(), UUID.randomUUID(), 0)));
        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), actor);

        assertCode(() -> service.crear(EMAIL, null), ErrorCode.INVITACION_ACTIVA_EXISTE);
        verify(invitations, never()).insert(any(), any(), any(), any());
    }

    @Test
    void crearLiberaInvitacionVencidaYLuegoEnvia() {
        UUID empresaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        CurrentUser actor = actor(actorId, empresaId, "USUARIO_CREAR");

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findActiveByEmpresaAndEmail(eq(empresaId), eq(EMAIL)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, past(), 1, null, null, 0)));
        when(invitations.insert(eq(empresaId), eq(EMAIL), eq(actorId), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 0, null, null, 0));
        when(invitations.markEnviada(eq(invId), eq(empresaId), eq(0L), eq(userId), eq(miembroId), any(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 1, userId, miembroId, 1));

        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        when(members.create(eq(empresaId), eq(userId), isNull(), eq(false), eq(actorId)))
                .thenReturn(miembro(miembroId, empresaId, userId, EstadoMiembro.ACTIVO));

        SupabaseAuthAdminClient auth = mock(SupabaseAuthAdminClient.class);
        when(auth.invite(eq(EMAIL), anyString()))
                .thenReturn(new SupabaseAuthAdminClient.AdminUser(userId, true));
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        ejecutarTransaccion(transactions);

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                auth, fluentJdbc(), transactions, mock(ApplicationEventPublisher.class), actor);

        InvitacionUsuario result = service.crear(EMAIL, null);

        verify(invitations).markExpired(any());
        assertThat(result.intentosEnvio()).isEqualTo(1);
        verify(auth).invite(eq(EMAIL), anyString());
    }

    @Test
    void crearMarcaErrorEnvioYReLanzaCuandoSupabaseFalla() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_CREAR");
        UUID invId = UUID.randomUUID();
        UUID empresaId = actor.empresaId();

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findActiveByEmpresaAndEmail(any(), eq(EMAIL))).thenReturn(Optional.empty());
        when(invitations.insert(any(), eq(EMAIL), any(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 0, null, null, 0));

        SupabaseAuthAdminClient auth = mock(SupabaseAuthAdminClient.class);
        when(auth.invite(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.SUPABASE_AUTH_UNAVAILABLE));

        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), auth, fluentJdbc(), mock(TransactionTemplate.class),
                mock(ApplicationEventPublisher.class), actor);

        assertCode(() -> service.crear(EMAIL, null), ErrorCode.SUPABASE_AUTH_UNAVAILABLE);
        verify(invitations).marcarErrorEnvio(eq(invId), eq(empresaId), eq(0L),
                eq("SUPABASE_AUTH_UNAVAILABLE"), anyString());
    }

    @Test
    void reenviarPermiteInvitacionVencida() {
        UUID empresaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        CurrentUser actor = actor(actorId, empresaId, "USUARIO_CREAR");

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(empresaId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.VENCIDA,
                        past(), 2, userId, miembroId, 0)));
        when(invitations.resend(eq(invId), eq(empresaId), eq(0L), any(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 3, userId, miembroId, 1));

        SupabaseAuthAdminClient auth = mock(SupabaseAuthAdminClient.class);
        when(auth.invite(eq(EMAIL), anyString()))
                .thenReturn(new SupabaseAuthAdminClient.AdminUser(userId, false));
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        ejecutarTransaccion(transactions);

        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), auth, fluentJdbc(), transactions,
                mock(ApplicationEventPublisher.class), actor);

        InvitacionUsuario result = service.reenviar(invId, 0L);

        assertThat(result.estado()).isEqualTo(EstadoInvitacion.PENDIENTE);
        assertThat(result.intentosEnvio()).isEqualTo(3);
        verify(invitations).resend(eq(invId), eq(empresaId), eq(0L), any(), any());
    }

    @Test
    void reenviarRechazaInvitacionAceptada() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_CREAR");
        UUID invId = UUID.randomUUID();
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(actor.empresaId())))
                .thenReturn(Optional.of(invitacion(invId, actor.empresaId(),
                        EstadoInvitacion.ACEPTADA, future(), 1, UUID.randomUUID(), UUID.randomUUID(), 1)));
        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), actor);

        assertCode(() -> service.reenviar(invId, 0L), ErrorCode.INVITACION_YA_ACEPTADA);
        verify(invitations, never()).resend(any(), any(), anyLong(), any(), any());
    }

    @Test
    void reenviarRechazaAlAlcanzarElLimite() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_CREAR");
        UUID invId = UUID.randomUUID();
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(actor.empresaId())))
                .thenReturn(Optional.of(invitacion(invId, actor.empresaId(),
                        EstadoInvitacion.PENDIENTE, future(), 5, UUID.randomUUID(), UUID.randomUUID(), 0)));
        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), actor);

        assertCode(() -> service.reenviar(invId, 0L), ErrorCode.INVITACION_LIMITE_REENVIOS);
        verify(invitations, never()).resend(any(), any(), anyLong(), any(), any());
    }

    @Test
    void reenviarVuelveAEnviarSinRecrearArtefactos() {
        UUID empresaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        CurrentUser actor = actor(actorId, empresaId, "USUARIO_CREAR");

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(empresaId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        future(), 2, userId, miembroId, 0)));
        when(invitations.resend(eq(invId), eq(empresaId), eq(4L), any(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 3, userId, miembroId, 1));

        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        SupabaseAuthAdminClient auth = mock(SupabaseAuthAdminClient.class);
        when(auth.invite(eq(EMAIL), anyString()))
                .thenReturn(new SupabaseAuthAdminClient.AdminUser(userId, true));
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        ejecutarTransaccion(transactions);

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                auth, fluentJdbc(), transactions, mock(ApplicationEventPublisher.class), actor);

        InvitacionUsuario result = service.reenviar(invId, 4L);

        assertThat(result.intentosEnvio()).isEqualTo(3);
        verify(invitations).resend(eq(invId), eq(empresaId), eq(4L), any(), any());
        verify(members, never()).create(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void cancelarCancelaInvitacion() {
        UUID empresaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        CurrentUser actor = actor(actorId, empresaId, "USUARIO_CREAR");

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(empresaId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        future(), 1, UUID.randomUUID(), UUID.randomUUID(), 0)));
        when(invitations.cancel(eq(invId), eq(empresaId), eq(0L), eq(actorId), isNull(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.CANCELADA, future(), 1,
                        UUID.randomUUID(), UUID.randomUUID(), 1));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), events, actor);

        InvitacionUsuario result = service.cancelar(invId, null, 0L);

        assertThat(result.estado()).isEqualTo(EstadoInvitacion.CANCELADA);
        verify(events).publishEvent(any(SeguridadAuditEvent.class));
    }

    @Test
    void cancelarRechazaInvitacionYaAceptada() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_CREAR");
        UUID invId = UUID.randomUUID();
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(actor.empresaId())))
                .thenReturn(Optional.of(invitacion(invId, actor.empresaId(),
                        EstadoInvitacion.ACEPTADA, future(), 1, UUID.randomUUID(), UUID.randomUUID(), 1)));
        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), actor);

        assertCode(() -> service.cancelar(invId, null, 0L), ErrorCode.INVITACION_YA_ACEPTADA);
    }

    @Test
    void cancelarBloqueaLaMembresiaInvitada() {
        UUID empresaId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        CurrentUser actor = actor(actorId, empresaId, "USUARIO_CREAR");

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(empresaId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        future(), 1, UUID.randomUUID(), miembroId, 0)));
        when(invitations.cancel(eq(invId), eq(empresaId), eq(3L), eq(actorId), anyString(), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.CANCELADA, future(), 1,
                        UUID.randomUUID(), miembroId, 1));

        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        when(members.findByIdAndEmpresaId(eq(miembroId), eq(empresaId)))
                .thenReturn(Optional.of(miembro(miembroId, empresaId, UUID.randomUUID(), EstadoMiembro.INVITADO)));

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                mock(SupabaseAuthAdminClient.class), fluentJdbc(), mock(TransactionTemplate.class),
                mock(ApplicationEventPublisher.class), actor);

        InvitacionUsuario result = service.cancelar(invId, "El correo era incorrecto", 3L);

        assertThat(result.estado()).isEqualTo(EstadoInvitacion.CANCELADA);
        verify(members).changeStatus(eq(miembroId), eq(empresaId), eq(EstadoMiembro.BLOQUEADO), eq(0L), eq(actorId));
        verify(invitations).cancel(eq(invId), eq(empresaId), eq(3L), eq(actorId), anyString(), any());
    }

    @Test
    void consultarMarcaVencidaCuandoExpiro() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_VER");
        UUID invId = UUID.randomUUID();
        UUID empresaId = actor.empresaId();
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByIdAndEmpresaId(eq(invId), eq(empresaId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        past(), 1, UUID.randomUUID(), UUID.randomUUID(), 0)));
        when(invitations.expire(eq(invId), eq(empresaId), eq(0L), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.VENCIDA,
                        past(), 1, UUID.randomUUID(), UUID.randomUUID(), 1));
        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), actor);

        InvitacionUsuario result = service.consultar(invId);

        assertThat(result.estado()).isEqualTo(EstadoInvitacion.VENCIDA);
        verify(invitations).expire(eq(invId), eq(empresaId), eq(0L), any());
    }

    @Test
    void expirePendingInvitationsVenceYAudita() {
        UUID empresaId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();
        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findPendingExpired(any())).thenReturn(List.of(
                invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, past(), 1,
                        UUID.randomUUID(), UUID.randomUUID(), 0)));
        when(invitations.expire(eq(invId), eq(empresaId), eq(0L), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.VENCIDA, past(), 1,
                        UUID.randomUUID(), UUID.randomUUID(), 1));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), events, null);

        service.expirePendingInvitations();

        verify(invitations).expire(eq(invId), eq(empresaId), eq(0L), any());
        verify(events).publishEvent(any(SeguridadAuditEvent.class));
    }

    @Test
    void aceptarActivaLaMembresiaYLaInvitacion() {
        UUID empresaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByUsuarioId(eq(userId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        future(), 1, userId, miembroId, 0)));
        when(invitations.accept(eq(invId), eq(empresaId), eq(0L), any()))
                .thenReturn(invitacion(invId, empresaId, EstadoInvitacion.ACEPTADA,
                        future(), 1, userId, miembroId, 1));

        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        when(members.findByIdAndEmpresaId(eq(miembroId), eq(empresaId)))
                .thenReturn(Optional.of(miembro(miembroId, empresaId, userId, EstadoMiembro.INVITADO)));
        when(members.findCurrentUser(eq(userId), eq(empresaId)))
                .thenReturn(Optional.of(new UsuarioActual(userId, "Invitado", "Pendiente", empresaId,
                        "Empresa X", Set.of("ADMINISTRADOR"), Set.of("USUARIO_VER"), Set.of())));

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                mock(SupabaseAuthAdminClient.class), fluentJdbc(), mock(TransactionTemplate.class),
                mock(ApplicationEventPublisher.class), null);

        ActivacionInvitacionResponse result = service.aceptar(userId.toString());

        assertThat(result.invitacion().estado()).isEqualTo(EstadoInvitacion.ACEPTADA);
        assertThat(result.nombreEmpresa()).isEqualTo("Empresa X");
        assertThat(result.roles()).contains("ADMINISTRADOR");
        assertThat(result.permisos()).contains("USUARIO_VER");
        verify(members).changeStatus(eq(miembroId), eq(empresaId), eq(EstadoMiembro.ACTIVO), eq(0L), eq(userId));
        verify(invitations).accept(eq(invId), eq(empresaId), eq(0L), any());
    }

    @Test
    void aceptarRechazaInvitacionVencida() {
        UUID userId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByUsuarioId(eq(userId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        past(), 1, userId, UUID.randomUUID(), 0)));
        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                mock(SupabaseAuthAdminClient.class), fluentJdbc(), mock(TransactionTemplate.class),
                mock(ApplicationEventPublisher.class), null);

        assertCode(() -> service.aceptar(userId.toString()), ErrorCode.INVITACION_VENCIDA);
        verify(members, never()).changeStatus(any(), any(), any(), anyLong(), any());
    }

    @Test
    void aceptarRechazaInvitacionCancelada() {
        UUID userId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByUsuarioId(eq(userId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.CANCELADA,
                        future(), 1, userId, UUID.randomUUID(), 1)));
        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), null);

        assertCode(() -> service.aceptar(userId.toString()), ErrorCode.INVITACION_CANCELADA);
    }

    @Test
    void aceptarRechazaCuandoLaMembresiaNoEstaInvitada() {
        UUID userId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UUID miembroId = UUID.randomUUID();
        UUID invId = UUID.randomUUID();

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.findByUsuarioId(eq(userId)))
                .thenReturn(Optional.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE,
                        future(), 1, userId, miembroId, 0)));
        MiembroEmpresaRepository members = mock(MiembroEmpresaRepository.class);
        when(members.findByIdAndEmpresaId(eq(miembroId), eq(empresaId)))
                .thenReturn(Optional.of(miembro(miembroId, empresaId, userId, EstadoMiembro.ACTIVO)));

        InvitacionService service = service(invitations, members, mock(PerfilUsuarioRepository.class),
                mock(SupabaseAuthAdminClient.class), fluentJdbc(), mock(TransactionTemplate.class),
                mock(ApplicationEventPublisher.class), null);

        assertCode(() -> service.aceptar(userId.toString()), ErrorCode.INVITACION_ESTADO_INVALIDO);
        verify(invitations, never()).accept(any(), any(), anyLong(), any());
    }

    @Test
    void listarDevuelvePaginaConFiltros() {
        CurrentUser actor = actor(UUID.randomUUID(), UUID.randomUUID(), "USUARIO_VER");
        UUID empresaId = actor.empresaId();
        UUID invId = UUID.randomUUID();

        InvitacionRepository invitations = mock(InvitacionRepository.class);
        when(invitations.search(eq(empresaId), any(InvitacionFiltro.class)))
                .thenReturn(List.of(invitacion(invId, empresaId, EstadoInvitacion.PENDIENTE, future(), 1,
                        UUID.randomUUID(), UUID.randomUUID(), 0)));
        when(invitations.count(eq(empresaId), any(InvitacionFiltro.class))).thenReturn(1L);

        InvitacionService service = service(invitations, mock(MiembroEmpresaRepository.class),
                mock(PerfilUsuarioRepository.class), mock(SupabaseAuthAdminClient.class), fluentJdbc(),
                mock(TransactionTemplate.class), mock(ApplicationEventPublisher.class), actor);

        InvitacionPage pagina = service.listar("PENDIENTE", "invitado", null, null, 0, 10);

        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.items()).hasSize(1);
        assertThat(pagina.items().get(0).id()).isEqualTo(invId);
    }

    @SuppressWarnings("unchecked")
    private void ejecutarTransaccion(TransactionTemplate transactions) {
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private InvitacionService service(InvitacionRepository invitations, MiembroEmpresaRepository members,
                                      PerfilUsuarioRepository profiles, SupabaseAuthAdminClient auth,
                                      JdbcClient jdbc, TransactionTemplate transactions,
                                      ApplicationEventPublisher events, CurrentUser actor) {
        AppProperties properties = new AppProperties(new AppProperties.Bootstrap(false, "token"),
                new AppProperties.SystemStatus(false), "http://localhost:5173",
                new AppProperties.Storage("bucket", 1024, Duration.ofMinutes(5),
                        List.of("image/png"), List.of("png")));
        return new InvitacionService(invitations, members, profiles, auth, properties,
                new InvitationProperties(72, 5), new UserContext(() -> actor), jdbc, transactions, events);
    }

    private JdbcClient fluentJdbc() {
        JdbcClient jdbc = mock(JdbcClient.class);
        JdbcClient.StatementSpec spec = mock(JdbcClient.StatementSpec.class);
        when(jdbc.sql(anyString())).thenReturn(spec);
        when(spec.param(anyString(), any())).thenReturn(spec);
        when(spec.update()).thenReturn(1);
        return jdbc;
    }

    private CurrentUser actor(UUID userId, UUID empresaId, String permiso) {
        return new CurrentUser(userId, empresaId, UUID.randomUUID(), Set.of("ADMINISTRADOR"),
                Set.of(permiso), Set.of(), false);
    }

    private InvitacionUsuario invitacion(UUID id, UUID empresaId, EstadoInvitacion estado,
                                         Instant vencimiento, int intentos, UUID usuarioId, UUID miembroId, long version) {
        return new InvitacionUsuario(id, empresaId, miembroId, usuarioId, EMAIL, estado,
                Instant.now(), vencimiento, null, null, intentos, null, null,
                UUID.randomUUID(), null, null, Instant.now(), Instant.now(), version);
    }

    private MiembroEmpresa miembro(UUID id, UUID empresaId, UUID usuarioId, EstadoMiembro estado) {
        PerfilUsuario profile = new PerfilUsuario(usuarioId, "Invitado", "Pendiente", null, null,
                true, null, Instant.now(), Instant.now(), 0);
        return new MiembroEmpresa(id, empresaId, profile, "cargo", estado, LocalDate.now(), false,
                Instant.now(), null, Instant.now(), null, 0, Set.of(), Set.of());
    }

    private Instant future() {
        return Instant.now().plus(Duration.ofHours(72));
    }

    private Instant past() {
        return Instant.now().minus(Duration.ofHours(1));
    }

    private void assertCode(ThrowingCallable callable, ErrorCode code) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(code));
    }

    @FunctionalInterface
    interface ThrowingCallable { void call(); }
}
