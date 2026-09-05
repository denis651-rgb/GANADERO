package bo.com.ganadero.animales.application;

/** Datos editables de un rango de categoría por edad (alta o edición desde Mi finca). */
public record RangoCategoriaCommand(String codigo, String nombre, String sexoAplicable, Integer edadMinMeses,
                                    Integer edadMaxMeses, String descripcion, boolean clasificacionAutomatica,
                                    int ordenEvaluacion, boolean confirmarHueco) {
}
