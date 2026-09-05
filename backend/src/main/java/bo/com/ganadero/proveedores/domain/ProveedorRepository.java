package bo.com.ganadero.proveedores.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProveedorRepository {
    List<Proveedor> buscar(String query, boolean soloActivos);
    Optional<Proveedor> findById(UUID id);
    Optional<Proveedor> findByDocumento(String documento);
    Proveedor crear(Proveedor proveedor, UUID actor);
    Proveedor actualizar(Proveedor proveedor, UUID actor);
    Proveedor cambiarEstado(UUID id, boolean activo, long version, UUID actor);
}
