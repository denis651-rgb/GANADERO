package bo.com.ganadero.movimientolote.application;

import bo.com.ganadero.movimientolote.domain.PreparacionMovimientoLote;
import bo.com.ganadero.movimientolote.domain.PreparacionMovimientoLoteMiembro;

import java.util.List;

public record PreparacionResultado(
        PreparacionMovimientoLote preparacion,
        List<PreparacionMovimientoLoteMiembro> miembros,
        int totalEncontrados,
        int totalElegibles,
        int totalExcluidos) {
}
