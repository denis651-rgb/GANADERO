package bo.com.ganadero.sanidad.api;
import bo.com.ganadero.sanidad.application.ConfirmarJornadaCommand;
import bo.com.ganadero.sanidad.domain.LugarAplicacion;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record ConfirmarJornadaRequest(@NotNull UUID operationId, @PositiveOrZero long version, @NotNull UUID planItemId,
                                      @Positive BigDecimal dosisAplicada, @Size(max = 500) String motivoAjusteDosis,
                                      @Size(max = 30) String unidadDosis, @Size(max = 200) String productoAplicadoTexto,
                                      @Size(max = 500) String motivoCambioProducto, @Size(max = 60) String viaAdministracion,
                                      LugarAplicacion lugarAplicacion, @NotNull @PastOrPresent LocalDate fechaAplicacion,
                                      @Size(max = 60) String resultado, @Size(max = 1000) String observaciones,
                                      @Min(0) Integer retiroCarneDias, @Min(0) Integer retiroLecheDias) {
    ConfirmarJornadaCommand command() {
        return new ConfirmarJornadaCommand(operationId, version, planItemId, dosisAplicada, motivoAjusteDosis,
                unidadDosis, productoAplicadoTexto, motivoCambioProducto, viaAdministracion, lugarAplicacion,
                fechaAplicacion, resultado, observaciones, retiroCarneDias, retiroLecheDias);
    }
}
