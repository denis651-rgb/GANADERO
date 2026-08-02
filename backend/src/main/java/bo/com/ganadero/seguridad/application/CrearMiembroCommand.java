package bo.com.ganadero.seguridad.application;

import java.util.Set;
import java.util.UUID;

public record CrearMiembroCommand(UUID usuarioId, String nombres, String apellidos, String telefono,
                                  String cargo, boolean accesoTodasPropiedades, Set<UUID> roles) {
    public CrearMiembroCommand { roles = Set.copyOf(roles); }
}
