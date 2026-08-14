package bo.com.ganadero.empresas.api;

import bo.com.ganadero.empresas.application.ActualizarConfiguracionCommand;
import bo.com.ganadero.empresas.domain.UnidadPeso;
import bo.com.ganadero.empresas.domain.UnidadSuperficie;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarConfiguracionRequest(
        UnidadPeso unidadPeso,
        UnidadSuperficie unidadSuperficie,
        @Size(min = 3, max = 3) String moneda,
        @Min(0) Integer diasAlertaPreparto,
        @Min(0) Integer diasAlertaVacunacion,
        @Min(1) Integer diasDiagnosticoPostServicio,
        @Min(1) Integer diasGestacionEstimada,
        @Min(0) Integer diasSinPesaje,
        Boolean permitirStockNegativo,
        Boolean requiereAprobacionVenta,
        Boolean comprimirImagenes,
        @Min(1) @Max(100) Integer calidadImagen,
        @NotNull Long version) {
    ActualizarConfiguracionCommand toCommand() {
        return new ActualizarConfiguracionCommand(unidadPeso, unidadSuperficie, moneda,
                diasAlertaPreparto, diasAlertaVacunacion, diasDiagnosticoPostServicio,
                diasGestacionEstimada, diasSinPesaje, permitirStockNegativo,
                requiereAprobacionVenta, comprimirImagenes, calidadImagen, version);
    }
}
