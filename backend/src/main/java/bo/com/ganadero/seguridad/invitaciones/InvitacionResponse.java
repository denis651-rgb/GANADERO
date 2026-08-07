package bo.com.ganadero.seguridad.invitaciones;

import java.time.Instant;
import java.util.UUID;

public record InvitacionResponse(
        UUID id,
        String email,
        EstadoInvitacion estado,
        Instant fechaEnvio,
        Instant fechaVencimiento,
        Instant fechaAceptacion,
        Instant fechaCancelacion,
        int intentosEnvio,
        String ultimoErrorCodigo,
        String ultimoErrorMensaje,
        String motivoCancelacion,
        UUID invitadoPor,
        long version) {

    public static InvitacionResponse from(InvitacionUsuario invitacion) {
        return new InvitacionResponse(
                invitacion.id(),
                invitacion.email(),
                invitacion.estado(),
                invitacion.fechaEnvio(),
                invitacion.fechaVencimiento(),
                invitacion.fechaAceptacion(),
                invitacion.fechaCancelacion(),
                invitacion.intentosEnvio(),
                invitacion.ultimoErrorCodigo(),
                invitacion.ultimoErrorMensaje(),
                invitacion.motivoCancelacion(),
                invitacion.invitadoPor(),
                invitacion.version());
    }
}
