package bo.com.ganadero.alertas.application;

/**
 * Categoría funcional estable de una alerta. Se utiliza para decidir si el
 * usuario desea recibir el canal Push, sin ocultar la alerta del centro.
 */
public enum CategoriaAlerta {
    REPRODUCCION,
    SANIDAD,
    TRATAMIENTO,
    PESAJE,
    MOVIMIENTO,
    INVENTARIO,
    SISTEMA
}
