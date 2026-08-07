package bo.com.ganadero.seguridad.invitaciones;

import java.time.Instant;
import java.util.UUID;

public record InvitacionUsuario(
        UUID id,
        UUID empresaId,
        UUID miembroEmpresaId,
        UUID usuarioId,
        String email,
        EstadoInvitacion estado,
        Instant fechaEnvio,
        Instant fechaVencimiento,
        Instant fechaAceptacion,
        Instant fechaCancelacion,
        int intentosEnvio,
        String ultimoErrorCodigo,
        String ultimoErrorMensaje,
        UUID invitadoPor,
        UUID canceladoPor,
        String motivoCancelacion,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public boolean vencida(Instant now) {
        return fechaVencimiento.isBefore(now)
                && (estado == EstadoInvitacion.PENDIENTE || estado == EstadoInvitacion.ERROR_ENVIO);
    }
}
