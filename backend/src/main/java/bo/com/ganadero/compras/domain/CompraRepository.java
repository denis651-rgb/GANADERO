package bo.com.ganadero.compras.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompraRepository {
    CompraPage list(EstadoCompra estado, int page, int size);
    Optional<Compra> findById(UUID id);
    Optional<Compra> findByAnimalId(UUID animalId);
    List<CompraDetalle> findDetalles(UUID compraId);
    Compra crear(Compra compra, List<CompraDetalle> detalles, UUID actor);
    Compra actualizarBorrador(Compra compra, List<CompraDetalle> detalles, UUID actor);
    void asignarAnimal(UUID detalleId, UUID animalId);
    Compra confirmar(UUID id, long version, UUID actor);
    Compra anular(UUID id, String motivo, long version, UUID actor);
    List<DependenciaCompra> dependenciasPosteriores(UUID animalId, UUID compraId);
}
