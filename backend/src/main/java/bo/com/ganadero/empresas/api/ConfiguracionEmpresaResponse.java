package bo.com.ganadero.empresas.api;

import bo.com.ganadero.empresas.domain.ConfiguracionEmpresa;
import bo.com.ganadero.empresas.domain.UnidadPeso;
import bo.com.ganadero.empresas.domain.UnidadSuperficie;

import java.time.Instant;
import java.util.UUID;

public record ConfiguracionEmpresaResponse(UUID empresaId, UnidadPeso unidadPeso,
        UnidadSuperficie unidadSuperficie, String moneda, int diasAlertaPreparto,
        int diasAlertaVacunacion, int diasDiagnosticoPostServicio, int diasGestacionEstimada,
        int diasSinPesaje, boolean permitirStockNegativo,
        boolean requiereAprobacionVenta, boolean comprimirImagenes, int calidadImagen,
        Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy, long version) {
    static ConfiguracionEmpresaResponse from(ConfiguracionEmpresa source) {
        return new ConfiguracionEmpresaResponse(source.empresaId(), source.unidadPeso(),
                source.unidadSuperficie(), source.moneda(), source.diasAlertaPreparto(),
                source.diasAlertaVacunacion(), source.diasDiagnosticoPostServicio(),
                source.diasGestacionEstimada(), source.diasSinPesaje(), source.permitirStockNegativo(),
                source.requiereAprobacionVenta(), source.comprimirImagenes(), source.calidadImagen(),
                source.createdAt(), source.createdBy(), source.updatedAt(), source.updatedBy(), source.version());
    }
}
