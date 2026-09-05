package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.PreparacionResultado;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PreparacionMovimientoLoteResponse(
        UUID id,
        UUID loteOrigenId,
        UUID propiedadOrigenId,
        UUID potreroOrigenId,
        String modalidad,
        UUID destinoPropiedadId,
        UUID destinoPotreroId,
        String accionLote,
        UUID loteDestinoId,
        String nuevoLoteNombre,
        Instant fechaEfectiva,
        String motivo,
        String observaciones,
        String estado,
        Instant fechaCaptura,
        Instant fechaExpiracion,
        long version,
        int totalEncontrados,
        int totalElegibles,
        int totalExcluidos,
        List<MiembroPreparacionResponse> miembros) {
    public static PreparacionMovimientoLoteResponse from(PreparacionResultado r) {
        var p = r.preparacion();
        return new PreparacionMovimientoLoteResponse(p.id(), p.loteOrigenId(), p.propiedadOrigenId(), p.potreroOrigenId(),
                p.modalidad() == null ? null : p.modalidad().name(), p.destinoPropiedadId(), p.destinoPotreroId(),
                p.accionLote() == null ? null : p.accionLote().name(), p.loteDestinoId(), p.nuevoLoteNombre(),
                p.fechaEfectiva(), p.motivo(), p.observaciones(), p.estado() == null ? null : p.estado().name(),
                p.fechaCaptura(), p.fechaExpiracion(), p.version(), r.totalEncontrados(), r.totalElegibles(),
                r.totalExcluidos(), r.miembros().stream().map(MiembroPreparacionResponse::from).toList());
    }
}
