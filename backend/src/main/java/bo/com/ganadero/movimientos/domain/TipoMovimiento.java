package bo.com.ganadero.movimientos.domain;

public enum TipoMovimiento {
    CAMBIO_POTRERO,
    CAMBIO_LOTE,
    TRANSFERENCIA_PROPIEDAD,
    INGRESO_COMPRA,
    SALIDA_VENTA,
    CUARENTENA,
    RETORNO_CUARENTENA;

    public boolean esReversible() {
        return this != INGRESO_COMPRA;
    }

    public TipoMovimiento inverso() {
        return switch (this) {
            case CAMBIO_POTRERO, CAMBIO_LOTE, TRANSFERENCIA_PROPIEDAD -> this;
            case SALIDA_VENTA -> INGRESO_COMPRA;
            case CUARENTENA -> RETORNO_CUARENTENA;
            case RETORNO_CUARENTENA -> CUARENTENA;
            case INGRESO_COMPRA -> throw new IllegalStateException("INGRESO_COMPRA no admite reversión");
        };
    }
}
