package bo.com.ganadero.respaldos.api;

import bo.com.ganadero.respaldos.domain.EstadoRespaldo;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoExternoRequest(@NotNull EstadoRespaldo estado, String error) {
}
