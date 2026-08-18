package bo.com.ganadero.alertas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PushTestRequest(
        @NotNull UUID suscripcionId,
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 500) String mensaje
) {}
