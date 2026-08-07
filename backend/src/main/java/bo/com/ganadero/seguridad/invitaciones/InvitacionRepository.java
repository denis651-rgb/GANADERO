package bo.com.ganadero.seguridad.invitaciones;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitacionRepository {
    InvitacionUsuario insert(UUID empresaId, String email, UUID invitadoPor, Instant fechaVencimiento);

    Optional<InvitacionUsuario> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<InvitacionUsuario> findActiveByEmpresaAndEmail(UUID empresaId, String email);

    Optional<InvitacionUsuario> findByUsuarioId(UUID usuarioId);

    List<InvitacionUsuario> search(UUID empresaId, InvitacionFiltro filtro);

    long count(UUID empresaId, InvitacionFiltro filtro);

    InvitacionUsuario markEnviada(UUID id, UUID empresaId, long version, UUID usuarioId,
                                  UUID miembroId, Instant fechaEnvio, Instant fechaVencimiento);

    InvitacionUsuario marcarErrorEnvio(UUID id, UUID empresaId, long version, String codigo, String mensaje);

    InvitacionUsuario resend(UUID id, UUID empresaId, long version, Instant fechaEnvio, Instant fechaVencimiento);

    InvitacionUsuario cancel(UUID id, UUID empresaId, long version, UUID canceladoPor,
                             String motivo, Instant ahora);

    InvitacionUsuario accept(UUID id, UUID empresaId, long version, Instant ahora);

    InvitacionUsuario expire(UUID id, UUID empresaId, long version, Instant ahora);

    List<InvitacionUsuario> findPendingExpired(Instant now);

    int markExpired(Instant now);
}
