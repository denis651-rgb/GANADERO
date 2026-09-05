package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.pesajes.domain.TipoPesaje;
import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeMasivoItem(
        UUID id,
        UUID animalId,
        LocalDate fecha,
        BigDecimal pesoKg,
        TipoPesaje tipo,
        TipoPeso tipoPeso,
        BigDecimal condicionCorporal,
        String bascula,
        UUID propiedadId,
        UUID potreroId,
        UUID loteId,
        String observaciones) {

    /** Compatibilidad con el shape anterior (sin tipoPeso). */
    public PesajeMasivoItem(UUID id, UUID animalId, LocalDate fecha, BigDecimal pesoKg, TipoPesaje tipo,
                            BigDecimal condicionCorporal, String bascula, UUID propiedadId, UUID potreroId,
                            UUID loteId, String observaciones) {
        this(id, animalId, fecha, pesoKg, tipo, null, condicionCorporal, bascula, propiedadId, potreroId,
                loteId, observaciones);
    }
}
