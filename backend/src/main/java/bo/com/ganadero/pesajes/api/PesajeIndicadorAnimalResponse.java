package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.EvolucionPesaje;
import bo.com.ganadero.pesajes.domain.PesajeIndicadorAnimal;
import bo.com.ganadero.pesajes.domain.TipoPesaje;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PesajeIndicadorAnimalResponse(
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
        List<EvolucionResponse> evolucion) {

    public record EvolucionResponse(LocalDate fecha, BigDecimal pesoKg, BigDecimal condicionCorporal,
                                    TipoPesaje tipo) {
        static EvolucionResponse from(EvolucionPesaje e) {
            return new EvolucionResponse(e.fecha(), e.pesoKg(), e.condicionCorporal(), e.tipo());
        }
    }

    public static PesajeIndicadorAnimalResponse from(PesajeIndicadorAnimal i) {
        return new PesajeIndicadorAnimalResponse(i.animalId(), i.codigoAnimal(), i.nombreAnimal(),
                i.ultimoPesoKg(), i.fechaUltimoPesaje(), i.pesoAnteriorKg(), i.fechaPesoAnterior(),
                i.variacionKg(), i.variacionPct(), i.gananciaDiariaKg(), i.promedioLoteKg(),
                i.animalesPesadosLote(), i.diferenciaVsLoteKg(), i.diferenciaVsLotePct(),
                i.evolucion().stream().map(EvolucionResponse::from).toList());
    }
}
