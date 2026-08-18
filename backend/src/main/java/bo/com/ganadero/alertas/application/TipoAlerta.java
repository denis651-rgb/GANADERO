package bo.com.ganadero.alertas.application;

public enum TipoAlerta {
    CELO_DETECTADO(CategoriaAlerta.REPRODUCCION),
    DIAGNOSTICO_PENDIENTE(CategoriaAlerta.REPRODUCCION),
    PARTO_PROXIMO(CategoriaAlerta.REPRODUCCION),
    DESTETE_PROXIMO(CategoriaAlerta.REPRODUCCION),

    VACUNA_PROXIMA(CategoriaAlerta.SANIDAD),
    VACUNA_VENCIDA(CategoriaAlerta.SANIDAD),
    RETIRO_CARNE_VIGENTE(CategoriaAlerta.SANIDAD),
    RETIRO_LECHE_VIGENTE(CategoriaAlerta.SANIDAD),
    CUARENTENA_POR_FINALIZAR(CategoriaAlerta.SANIDAD),
    CASO_CLINICO_CRITICO(CategoriaAlerta.SANIDAD),
    RECORDATORIO_SANIDAD(CategoriaAlerta.SANIDAD),

    TRATAMIENTO_PROXIMO(CategoriaAlerta.TRATAMIENTO),
    TRATAMIENTO_ATRASADO(CategoriaAlerta.TRATAMIENTO),
    PESAJE_ATRASADO(CategoriaAlerta.PESAJE),
    MOVIMIENTO_PENDIENTE(CategoriaAlerta.MOVIMIENTO),
    INVENTARIO_BAJO(CategoriaAlerta.INVENTARIO),
    SISTEMA_REQUIERE_ATENCION(CategoriaAlerta.SISTEMA);

    private final CategoriaAlerta categoria;

    TipoAlerta(CategoriaAlerta categoria) {
        this.categoria = categoria;
    }

    public CategoriaAlerta categoria() {
        return categoria;
    }
}
