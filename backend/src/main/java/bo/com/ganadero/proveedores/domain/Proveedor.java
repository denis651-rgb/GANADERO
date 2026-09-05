package bo.com.ganadero.proveedores.domain;

import java.time.Instant;
import java.util.UUID;

public record Proveedor(UUID id, String nombre, String telefono, String documento, String direccion,
                        String correo, String observaciones, boolean activo, Instant createdAt, UUID createdBy,
                        Instant updatedAt, UUID updatedBy, long version) {
}
