package bo.com.ganadero.movimientos.application;

import java.util.List;

public record ValidacionMovimiento(boolean valido, long total, long validos, long invalidos,
                                   List<ValidacionAnimalResult> resultados) {
    public static ValidacionMovimiento of(List<ValidacionAnimalResult> resultados) {
        long invalidos = resultados.stream().filter(resultado -> !resultado.valido()).count();
        return new ValidacionMovimiento(invalidos == 0, resultados.size(), resultados.size() - invalidos,
                invalidos, resultados);
    }
}
