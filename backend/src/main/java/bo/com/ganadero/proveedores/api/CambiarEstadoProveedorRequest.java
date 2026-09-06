package bo.com.ganadero.proveedores.api;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoProveedorRequest(boolean activo, @NotNull Long version) {
}
