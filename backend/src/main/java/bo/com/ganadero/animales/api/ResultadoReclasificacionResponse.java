package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.ResultadoReclasificacion;

public record ResultadoReclasificacionResponse(int procesados, int actualizados, int omitidos, int errores) {
    public static ResultadoReclasificacionResponse from(ResultadoReclasificacion r) {
        return new ResultadoReclasificacionResponse(r.procesados(), r.actualizados(), r.omitidos(), r.errores());
    }
}
