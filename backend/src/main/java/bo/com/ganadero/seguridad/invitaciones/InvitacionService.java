package bo.com.ganadero.seguridad.invitaciones;

import bo.com.ganadero.seguridad.application.SeguridadAuditEvent;
import bo.com.ganadero.seguridad.domain.EstadoMiembro;
import bo.com.ganadero.seguridad.domain.MiembroEmpresa;
import bo.com.ganadero.seguridad.domain.MiembroEmpresaRepository;
import bo.com.ganadero.seguridad.domain.PerfilUsuarioRepository;
import bo.com.ganadero.seguridad.domain.UsuarioActual;
import bo.com.ganadero.seguridad.infrastructure.SupabaseAuthAdminClient;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.config.InvitationProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitacionService {
    private static final Logger log = LoggerFactory.getLogger(InvitacionService.class);

    private final InvitacionRepository invitations;
    private final MiembroEmpresaRepository members;
    private final PerfilUsuarioRepository profiles;
    private final SupabaseAuthAdminClient auth;
    private final AppProperties properties;
    private final InvitationProperties invitationProperties;
    private final UserContext context;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final ApplicationEventPublisher events;

    public InvitacionService(InvitacionRepository invitations, MiembroEmpresaRepository members,
                             PerfilUsuarioRepository profiles, SupabaseAuthAdminClient auth,
                             AppProperties properties, InvitationProperties invitationProperties,
                             UserContext context, JdbcClient jdbc, TransactionTemplate transactions,
                             ApplicationEventPublisher events) {
        this.invitations = invitations;
        this.members = members;
        this.profiles = profiles;
        this.auth = auth;
        this.properties = properties;
        this.invitationProperties = invitationProperties;
        this.context = context;
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.events = events;
    }

    public InvitacionUsuario crear(String email, String cargo) {
        CurrentUser actor = context.requirePermission("USUARIO_CREAR");
        String emailNormalizado = email.trim().toLowerCase();
        if (members.existsActiveByEmail(actor.empresaId(), emailNormalizado)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_MEMBER);
        }
        Instant ahora = Instant.now();
        Optional<InvitacionUsuario> activa = invitations.findActiveByEmpresaAndEmail(actor.empresaId(), emailNormalizado);
        if (activa.isPresent()) {
            if (activa.get().vencida(ahora)) {
                invitations.markExpired(ahora);
            } else {
                throw new BusinessException(ErrorCode.INVITACION_ACTIVA_EXISTE);
            }
        }
        InvitacionUsuario inv;
        try {
            inv = invitations.insert(actor.empresaId(), emailNormalizado, actor.userId(),
                    ahora.plus(Duration.ofHours(invitationProperties.expirationHours())));
        } catch (DataIntegrityViolationException conflicto) {
            throw new BusinessException(ErrorCode.INVITACION_ACTIVA_EXISTE);
        }
        return enviar(inv, cargo, actor, ahora, inv.version(), false);
    }

    public InvitacionUsuario reenviar(UUID id, long version) {
        CurrentUser actor = context.requirePermission("USUARIO_CREAR");
        InvitacionUsuario inv = invitations.findByIdAndEmpresaId(id, actor.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITACION_NOT_FOUND));
        if (inv.estado() == EstadoInvitacion.ACEPTADA) throw new BusinessException(ErrorCode.INVITACION_YA_ACEPTADA);
        if (inv.estado() == EstadoInvitacion.CANCELADA) throw new BusinessException(ErrorCode.INVITACION_CANCELADA);
        if (inv.intentosEnvio() >= invitationProperties.maxResendAttempts()) {
            throw new BusinessException(ErrorCode.INVITACION_LIMITE_REENVIOS);
        }
        return enviar(inv, null, actor, Instant.now(), version, true);
    }

    @Transactional
    public InvitacionUsuario cancelar(UUID id, String motivo, long version) {
        CurrentUser actor = context.requirePermission("USUARIO_CREAR");
        InvitacionUsuario inv = invitations.findByIdAndEmpresaId(id, actor.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITACION_NOT_FOUND));
        Instant ahora = Instant.now();
        if (inv.estado() == EstadoInvitacion.ACEPTADA) throw new BusinessException(ErrorCode.INVITACION_YA_ACEPTADA);
        if (inv.estado() == EstadoInvitacion.CANCELADA) throw new BusinessException(ErrorCode.INVITACION_ESTADO_INVALIDO);
        if (inv.miembroEmpresaId() != null) {
            members.findByIdAndEmpresaId(inv.miembroEmpresaId(), inv.empresaId())
                    .filter(m -> m.estado() == EstadoMiembro.INVITADO)
                    .ifPresent(m -> members.changeStatus(m.id(), inv.empresaId(),
                            EstadoMiembro.BLOQUEADO, m.version(), actor.userId()));
        }
        InvitacionUsuario cancelada = invitations.cancel(id, actor.empresaId(), version, actor.userId(), motivo, ahora);
        events.publishEvent(new SeguridadAuditEvent(actor.empresaId(), actor.userId(),
                "CANCELAR_INVITACION", "INVITACION_USUARIO", id, Instant.now()));
        return cancelada;
    }

    @Transactional
    public InvitacionUsuario consultar(UUID id) {
        CurrentUser actor = context.requirePermission("USUARIO_VER");
        InvitacionUsuario inv = invitations.findByIdAndEmpresaId(id, actor.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITACION_NOT_FOUND));
        if (inv.vencida(Instant.now())) {
            return invitations.expire(id, inv.empresaId(), inv.version(), Instant.now());
        }
        return inv;
    }

    public InvitacionPage listar(String estado, String email, OffsetDateTime desde, OffsetDateTime hasta,
                                 int page, int size) {
        CurrentUser actor = context.requirePermission("USUARIO_VER");
        InvitacionFiltro filtro = new InvitacionFiltro(estado, email, desde, hasta, page, size);
        List<InvitacionResponse> items = invitations.search(actor.empresaId(), filtro).stream()
                .map(InvitacionResponse::from).toList();
        long total = invitations.count(actor.empresaId(), filtro);
        return new InvitacionPage(items, total, filtro.page(), filtro.size());
    }

    @Transactional
    public ActivacionInvitacionResponse aceptar(String jwtSubject) {
        UUID usuarioId = parseUuid(jwtSubject);
        InvitacionUsuario inv = invitations.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITACION_NOT_FOUND));
        Instant ahora = Instant.now();
        if (inv.estado() == EstadoInvitacion.CANCELADA) throw new BusinessException(ErrorCode.INVITACION_CANCELADA);
        if (inv.estado() == EstadoInvitacion.ACEPTADA) throw new BusinessException(ErrorCode.INVITACION_YA_ACEPTADA);
        if (inv.vencida(ahora)) throw new BusinessException(ErrorCode.INVITACION_VENCIDA);
        MiembroEmpresa miembro = members.findByIdAndEmpresaId(inv.miembroEmpresaId(), inv.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITACION_ESTADO_INVALIDO));
        if (miembro.estado() != EstadoMiembro.INVITADO) {
            throw new BusinessException(ErrorCode.INVITACION_ESTADO_INVALIDO);
        }
        members.changeStatus(miembro.id(), inv.empresaId(), EstadoMiembro.ACTIVO, miembro.version(), usuarioId);
        InvitacionUsuario aceptada = invitations.accept(inv.id(), inv.empresaId(), inv.version(), ahora);
        events.publishEvent(new SeguridadAuditEvent(inv.empresaId(), usuarioId,
                "ACEPTAR_INVITACION", "INVITACION_USUARIO", inv.id(), Instant.now()));
        UsuarioActual actual = members.findCurrentUser(usuarioId, inv.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        return ActivacionInvitacionResponse.from(aceptada, actual);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void expirePendingInvitations() {
        Instant ahora = Instant.now();
        for (InvitacionUsuario inv : invitations.findPendingExpired(ahora)) {
            try {
                invitations.expire(inv.id(), inv.empresaId(), inv.version(), ahora);
                events.publishEvent(new SeguridadAuditEvent(inv.empresaId(), null,
                        "VENCER_INVITACION", "INVITACION_USUARIO", inv.id(), ahora));
                log.info("Invitación {} marcada como vencida", inv.id());
            } catch (RuntimeException fallo) {
                log.warn("No se pudo vencer la invitación {}", inv.id(), fallo);
            }
        }
    }

    private InvitacionUsuario enviar(InvitacionUsuario inv, String cargo, CurrentUser actor,
                                     Instant ahora, long version, boolean reenviando) {
        SupabaseAuthAdminClient.AdminUser user;
        try {
            user = auth.invite(inv.email(), properties.frontendUrl() + "/auth/activar-invitacion");
        } catch (RuntimeException fallo) {
            marcarError(inv, fallo);
            throw fallo;
        }
        Instant vencimiento = ahora.plus(Duration.ofHours(invitationProperties.expirationHours()));
        try {
            InvitacionUsuario enviada = transactions.execute(status -> {
                UUID miembroId;
                if (inv.usuarioId() == null) {
                    MiembroEmpresa miembro = crearArtefactosLocales(actor.empresaId(), user.id(),
                            inv.email(), cargo, actor.userId());
                    miembroId = miembro.id();
                } else {
                    miembroId = inv.miembroEmpresaId();
                }
                if (reenviando) {
                    return invitations.resend(inv.id(), actor.empresaId(), version, ahora, vencimiento);
                }
                return invitations.markEnviada(inv.id(), actor.empresaId(), version,
                        user.id(), miembroId, ahora, vencimiento);
            });
            events.publishEvent(new SeguridadAuditEvent(actor.empresaId(), actor.userId(),
                    reenviando ? "REENVIAR_INVITACION" : "INVITAR", "INVITACION_USUARIO", inv.id(), Instant.now()));
            return enviada;
        } catch (DataIntegrityViolationException conflicto) {
            auth.deleteIfCreated(user);
            throw new BusinessException(ErrorCode.INVITACION_ACTIVA_EXISTE);
        } catch (RuntimeException local) {
            auth.deleteIfCreated(user);
            marcarError(inv, local);
            throw local;
        }
    }

    private MiembroEmpresa crearArtefactosLocales(UUID empresaId, UUID usuarioId, String email,
                                                  String cargo, UUID actorId) {
        profiles.createIfAbsent(usuarioId, "Invitado", "Pendiente", null, actorId);
        jdbc.sql("update seguridad.perfiles_usuario set email = :email where id = :id")
                .param("email", email).param("id", usuarioId).update();
        MiembroEmpresa miembro = members.create(empresaId, usuarioId, cargo, false, actorId);
        members.changeStatus(miembro.id(), empresaId, EstadoMiembro.INVITADO, miembro.version(), actorId);
        return miembro;
    }

    private void marcarError(InvitacionUsuario inv, RuntimeException fallo) {
        try {
            String codigo = fallo instanceof BusinessException negocio ? negocio.code().name() : "ERROR_INTERNO";
            invitations.marcarErrorEnvio(inv.id(), inv.empresaId(), inv.version(), codigo, fallo.getMessage());
            log.warn("Fallo el envío de la invitación id={} codigo={}", inv.id(), codigo);
        } catch (RuntimeException registro) {
            log.warn("No se pudo registrar el fallo de envío de la invitación {}", inv.id(), registro);
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
