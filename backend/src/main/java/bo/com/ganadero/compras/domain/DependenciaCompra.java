package bo.com.ganadero.compras.domain;

/** Un evento posterior encontrado para un animal de la compra; bloquea la anulación directa. */
public record DependenciaCompra(String codigoAnimal, String tipo, String detalle) {
}
