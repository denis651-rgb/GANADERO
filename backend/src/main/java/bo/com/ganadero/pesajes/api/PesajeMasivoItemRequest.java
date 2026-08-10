package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.TipoPesaje;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeMasivoItemRequest(
        UUID id,
        @NotNull UUID animalId,
        LocalDate fecha,
        @NotNull @Positive BigDecimal pesoKg,
        TipoPesaje tipo,
        @jakarta.validation.constraints.DecimalMin("1.0")
        @jakarta.validation.constraints.DecimalMax("5.0") BigDecimal condicionCorporal,
        @Size(max = 120) String bascula,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        @Size(max = 1000) String observaciones) {
}
