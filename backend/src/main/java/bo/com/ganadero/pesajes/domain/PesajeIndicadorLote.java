package bo.com.ganadero.pesajes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeIndicadorLote(
        UUID loteId,
        String codigoLote,
        String nombreLote,
        Integer animalesTotales,
        Integer animalesPesados,
        Integer animalesSinPesaje,
        BigDecimal pesoPromedioKg,
        BigDecimal pesoMinimoKg,
        BigDecimal pesoMaximoKg,
        LocalDate fechaPrimerPesaje,
        LocalDate fechaUltimoPesaje) {
}
