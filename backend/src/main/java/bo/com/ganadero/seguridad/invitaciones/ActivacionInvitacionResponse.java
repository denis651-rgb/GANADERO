package bo.com.ganadero.seguridad.invitaciones;

import bo.com.ganadero.seguridad.domain.UsuarioActual;

import java.util.Set;
import java.util.UUID;

public record ActivacionInvitacionResponse(
        UUID usuarioId,
        String nombres,
        String apellidos,
        UUID empresaId,
        String nombreEmpresa,
        UUID miembroEmpresaId,
        Set<String> roles,
        Set<String> permisos,
        Set<UUID> propiedadesPermitidas,
        InvitacionResponse invitacion) {

    public static ActivacionInvitacionResponse from(InvitacionUsuario invitacion, UsuarioActual actual) {
        return new ActivacionInvitacionResponse(
                actual.usuarioId(),
                actual.nombres(),
                actual.apellidos(),
                actual.empresaId(),
                actual.empresaNombre(),
                invitacion.miembroEmpresaId(),
                actual.roles(),
                actual.permisos(),
                actual.propiedadesPermitidas(),
                InvitacionResponse.from(invitacion));
    }
}
