package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.PesajeIndicadorLote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeIndicadorLoteResponse(
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

    public static PesajeIndicadorLoteResponse from(PesajeIndicadorLote i) {
        return new PesajeIndicadorLoteResponse(i.loteId(), i.codigoLote(), i.nombreLote(), i.animalesTotales(),
                i.animalesPesados(), i.animalesSinPesaje(), i.pesoPromedioKg(), i.pesoMinimoKg(),
                i.pesoMaximoKg(), i.fechaPrimerPesaje(), i.fechaUltimoPesaje());
    }
}
