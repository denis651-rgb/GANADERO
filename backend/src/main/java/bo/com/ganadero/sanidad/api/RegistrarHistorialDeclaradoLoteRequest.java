package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;
import bo.com.ganadero.sanidad.application.RegistrarHistorialDeclaradoLoteCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegistrarHistorialDeclaradoLoteRequest(
        @NotEmpty @Size(max = 500) List<@NotNull UUID> animalIds,
        @NotEmpty @Size(max = 20) List<@Valid ActividadDeclarada> actividades) {

    public record ActividadDeclarada(
            @NotNull TipoActividadSanitaria tipoActividad,
            UUID planItemId,
            @NotNull @PastOrPresent LocalDate fechaAplicacion,
            @Positive BigDecimal dosis,
            @Size(max = 30) String unidadDosis,
            @Size(max = 300) String productoTexto,
            @Size(max = 1000) String observaciones) {
    }

    public RegistrarHistorialDeclaradoLoteCommand command() {
        return new RegistrarHistorialDeclaradoLoteCommand(animalIds, actividades.stream()
                .map(a -> new RegistrarHistorialDeclaradoLoteCommand.Actividad(a.tipoActividad,
                        a.planItemId, a.fechaAplicacion, a.dosis, a.unidadDosis,
                        a.productoTexto, a.observaciones))
                .toList());
    }
}
