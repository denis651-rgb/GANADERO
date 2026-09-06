package bo.com.ganadero.compras.api;

import bo.com.ganadero.compras.domain.DependenciaCompra;

public record DependenciaCompraResponse(String codigoAnimal, String tipo, String detalle) {
    public static DependenciaCompraResponse from(DependenciaCompra d) {
        return new DependenciaCompraResponse(d.codigoAnimal(), d.tipo(), d.detalle());
    }
}
