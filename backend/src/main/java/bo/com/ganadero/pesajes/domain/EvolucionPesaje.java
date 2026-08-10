package bo.com.ganadero.pesajes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EvolucionPesaje(
        LocalDate fecha,
        BigDecimal pesoKg,
        BigDecimal condicionCorporal,
        TipoPesaje tipo) {
}
