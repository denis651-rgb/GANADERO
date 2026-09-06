package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.EstadoPesaje;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.TipoPesaje;
import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PesajeResponse(
        UUID id,
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        LocalDate fecha,
        BigDecimal pesoKg,
        TipoPesaje tipo,
        TipoPeso tipoPeso,
        BigDecimal condicionCorporal,
        String bascula,
        UUID responsableId,
        String responsableNombre,
        UUID propiedadId,
        String propiedadNombre,
        UUID potreroId,
        String potreroNombre,
        UUID loteId,
        String loteNombre,
        String dispositivo,
        UUID compraId,
        UUID ventaId,
        UUID movimientoId,
        UUID clienteUuid,
        EstadoPesaje estado,
        String motivoAnulacion,
        String observaciones,
        long version) {

    public static PesajeResponse from(Pesaje p) {
        return new PesajeResponse(p.id(), p.animalId(), p.codigoAnimal(), p.nombreAnimal(), p.fecha(), p.pesoKg(),
                p.tipo(), p.tipoPeso(), p.condicionCorporal(), p.bascula(), p.responsableId(), p.responsableNombre(),
                p.propiedadId(), p.propiedadNombre(), p.potreroId(), p.potreroNombre(), p.loteId(), p.loteNombre(),
                p.dispositivo(), p.compraId(), p.ventaId(), p.movimientoId(), p.clienteUuid(), p.estado(),
                p.motivoAnulacion(), p.observaciones(), p.version());
    }
}
