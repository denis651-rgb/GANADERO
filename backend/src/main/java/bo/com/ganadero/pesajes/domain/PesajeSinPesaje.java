package bo.com.ganadero.pesajes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeSinPesaje(
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        LocalDate ultimoPesaje,
        BigDecimal pesoUltimoKg,
        long diasSinPesaje) {
}
