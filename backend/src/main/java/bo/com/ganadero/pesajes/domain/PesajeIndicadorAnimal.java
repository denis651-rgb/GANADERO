package bo.com.ganadero.pesajes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PesajeIndicadorAnimal(
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        BigDecimal ultimoPesoKg,
        LocalDate fechaUltimoPesaje,
        BigDecimal pesoAnteriorKg,
        LocalDate fechaPesoAnterior,
        BigDecimal variacionKg,
        BigDecimal variacionPct,
        BigDecimal gananciaDiariaKg,
        BigDecimal promedioLoteKg,
        Integer animalesPesadosLote,
        BigDecimal diferenciaVsLoteKg,
        BigDecimal diferenciaVsLotePct,
        List<EvolucionPesaje> evolucion) {
}
