package bo.com.ganadero.seguridad.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record MiembroEmpresa(UUID id, UUID empresaId, PerfilUsuario perfil, String cargo,
        EstadoMiembro estado, LocalDate fechaIngreso, boolean accesoTodasPropiedades,
        Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy, long version,
        Set<Rol> roles, Set<UUID> propiedadesPermitidas) {
    public MiembroEmpresa {
        roles = Set.copyOf(roles);
        propiedadesPermitidas = Set.copyOf(propiedadesPermitidas);
    }

    public boolean isOwner() { return roles.stream().anyMatch(role -> role.codigo().equals("PROPIETARIO")); }
}
