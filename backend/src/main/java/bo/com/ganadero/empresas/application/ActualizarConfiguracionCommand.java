package bo.com.ganadero.empresas.application;

import bo.com.ganadero.empresas.domain.UnidadPeso;
import bo.com.ganadero.empresas.domain.UnidadSuperficie;

public record ActualizarConfiguracionCommand(UnidadPeso unidadPeso, UnidadSuperficie unidadSuperficie,
        String moneda, Integer diasAlertaPreparto, Integer diasAlertaVacunacion, Integer diasSinPesaje,
        Boolean permitirStockNegativo, Boolean requiereAprobacionVenta, Boolean comprimirImagenes,
        Integer calidadImagen, long version) {}
