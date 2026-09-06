package bo.com.ganadero.proveedores.api;

import bo.com.ganadero.proveedores.application.ProveedorCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(@NotBlank @Size(max = 160) String nombre, @Size(max = 30) String telefono,
                               @Size(max = 30) String documento, @Size(max = 200) String direccion,
                               @Size(max = 160) String correo, @Size(max = 500) String observaciones,
                               Long version) {
    public ProveedorCommand command() {
        return new ProveedorCommand(null, nombre, telefono, documento, direccion, correo, observaciones, version);
    }
}
