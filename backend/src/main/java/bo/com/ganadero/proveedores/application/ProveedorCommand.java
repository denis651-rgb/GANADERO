package bo.com.ganadero.proveedores.application;

import java.util.UUID;

/** Datos para crear/actualizar un proveedor, o para "buscar-o-crear" al vuelo desde una compra. */
public record ProveedorCommand(UUID id, String nombre, String telefono, String documento, String direccion,
                               String correo, String observaciones, Long version) {
}
