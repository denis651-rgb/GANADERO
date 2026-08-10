package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.PesajeSinPesaje;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeSinPesajeResponse(
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        LocalDate ultimoPesaje,
        BigDecimal pesoUltimoKg,
        long diasSinPesaje) {

    public static PesajeSinPesajeResponse from(PesajeSinPesaje p) {
        return new PesajeSinPesajeResponse(p.animalId(), p.codigoAnimal(), p.nombreAnimal(),
                p.ultimoPesaje(), p.pesoUltimoKg(), p.diasSinPesaje());
    }
}
