package bo.com.ganadero.proveedores.api;

import bo.com.ganadero.proveedores.domain.Proveedor;

import java.util.UUID;

public record ProveedorResponse(UUID id, String nombre, String telefono, String documento, String direccion,
                                String correo, String observaciones, boolean activo, long version) {
    public static ProveedorResponse from(Proveedor p) {
        return new ProveedorResponse(p.id(), p.nombre(), p.telefono(), p.documento(), p.direccion(), p.correo(),
                p.observaciones(), p.activo(), p.version());
    }
}
