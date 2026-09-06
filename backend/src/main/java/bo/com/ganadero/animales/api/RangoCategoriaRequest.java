package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.RangoCategoriaCommand;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record RangoCategoriaRequest(@NotBlank @Size(max = 30) String codigo, @NotBlank @Size(max = 80) String nombre,
                                    @NotBlank String sexoAplicable, @PositiveOrZero Integer edadMinMeses,
                                    @PositiveOrZero Integer edadMaxMeses, @Size(max = 500) String descripcion,
                                    boolean clasificacionAutomatica, int ordenEvaluacion, boolean confirmarHueco,
                                    UUID id) {
    RangoCategoriaCommand command() {
        return new RangoCategoriaCommand(codigo, nombre, sexoAplicable, edadMinMeses, edadMaxMeses, descripcion,
                clasificacionAutomatica, ordenEvaluacion, confirmarHueco);
    }
}
