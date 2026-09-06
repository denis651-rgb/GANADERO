package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegistrarHistorialDeclaradoLoteCommand(
        List<UUID> animalIds,
        List<Actividad> actividades) {

    public record Actividad(
            TipoActividadSanitaria tipoActividad,
            UUID planItemId,
            LocalDate fechaAplicacion,
            BigDecimal dosis,
            String unidadDosis,
            String productoTexto,
            String observaciones) {
    }
}
