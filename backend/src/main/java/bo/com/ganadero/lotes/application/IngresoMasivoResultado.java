package bo.com.ganadero.lotes.application;

import java.util.List;

public record IngresoMasivoResultado(boolean ok, int procesados, int ingresados,
                                     List<ResultadoAccion> resultados) {
}
