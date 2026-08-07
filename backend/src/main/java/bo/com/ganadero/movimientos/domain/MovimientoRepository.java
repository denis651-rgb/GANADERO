package bo.com.ganadero.movimientos.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoRepository {
    MovimientoPage findAll(UUID empresa, EstadoMovimiento estado, TipoMovimiento tipo, int page, int size);
    Optional<Movimiento> findById(UUID id, UUID empresa);
    Optional<Movimiento> findByIdForUpdate(UUID id, UUID empresa);
    Optional<Movimiento> findByOriginal(UUID id, UUID empresa);
    List<MovimientoDetalle> findDetalles(UUID movimientoId);
    Movimiento create(Movimiento movimiento, List<MovimientoAnimal> animales, UUID actor);
    Movimiento saveConfirmed(Movimiento movimiento, List<MovimientoAnimal> animales, UUID actor);
    Movimiento confirm(UUID id, UUID empresa, long version, UUID actor);
    Movimiento annul(UUID id, UUID empresa, String motivo, long version, UUID actor);
    Movimiento markReverted(UUID id, UUID empresa, UUID reversionId, String motivo, long version, UUID actor);
    void saveDetalleUbicaciones(UUID movimientoId, List<MovimientoDetalle> detalle);
}
