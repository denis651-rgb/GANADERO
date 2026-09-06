package bo.com.ganadero.movimientolote.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreparacionMovimientoLoteRepository {
    PreparacionMovimientoLote crear(PreparacionMovimientoLote preparacion, List<PreparacionMovimientoLoteMiembro> miembros,
                                    UUID actor);
    Optional<PreparacionMovimientoLote> findById(UUID id);
    Optional<PreparacionMovimientoLote> findByIdForUpdate(UUID id);
    List<PreparacionMovimientoLoteMiembro> findMiembros(UUID preparacionId);
    PreparacionMovimientoLote marcarEstado(UUID id, EstadoPreparacionLote estado, long version, UUID actor);
    PreparacionMovimientoLote confirmar(UUID id, UUID movimientoResultanteId, UUID loteResultanteId, long version, UUID actor);
    void registrarAutorizacion(UUID preparacionId, UUID animalId, String tipoRestriccion, String motivo, UUID usuarioId);
    List<AutorizacionRestriccion> findAutorizaciones(UUID preparacionId);
}
