package bo.com.ganadero.lotes.application;

import java.util.List;

public record RetiroMasivoResultado(boolean ok, int procesados, int retirados,
                                    List<ResultadoAccion> resultados) {
}
