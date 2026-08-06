package bo.com.ganadero.empresas.domain;

import java.time.Instant;
import java.util.UUID;

public class Empresa {
    private final UUID id;
    private final String codigo;
    private String razonSocial;
    private String nombreComercial;
    private String nit;
    private String telefono;
    private String email;
    private String direccion;
    private final String zonaHoraria;
    private final String moneda;
    private final EstadoEmpresa estado;
    private final String logoPath;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private long version;

    public Empresa(UUID id, String codigo, String razonSocial, String nombreComercial, String nit,
                   String telefono, String email, String direccion, String zonaHoraria, String moneda,
                   EstadoEmpresa estado, String logoPath, Instant createdAt, UUID createdBy,
                   Instant updatedAt, UUID updatedBy, long version) {
        this.id = id; this.codigo = codigo; this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial; this.nit = nit; this.telefono = telefono;
        this.email = email; this.direccion = direccion; this.zonaHoraria = zonaHoraria;
        this.moneda = moneda; this.estado = estado; this.logoPath = logoPath;
        this.createdAt = createdAt; this.createdBy = createdBy; this.updatedAt = updatedAt;
        this.updatedBy = updatedBy; this.version = version;
    }

    public void update(String razonSocial, String nombreComercial, String nit, String telefono,
                       String email, String direccion, long expectedVersion, UUID actorId) {
        if (version != expectedVersion) throw new bo.com.ganadero.shared.error.BusinessException(
                bo.com.ganadero.shared.error.ErrorCode.VERSION_CONFLICT);
        if (razonSocial != null && razonSocial.isBlank()) throw invalid("La razón social no puede estar vacía.");
        if (nombreComercial != null && nombreComercial.isBlank()) throw invalid("El nombre comercial no puede estar vacío.");
        if (razonSocial != null) this.razonSocial = razonSocial;
        if (nombreComercial != null) this.nombreComercial = nombreComercial;
        if (nit != null) this.nit = nit;
        if (telefono != null) this.telefono = telefono;
        if (email != null) this.email = email;
        if (direccion != null) this.direccion = direccion;
        this.updatedBy = actorId;
    }

    private bo.com.ganadero.shared.error.BusinessException invalid(String message) {
        return new bo.com.ganadero.shared.error.BusinessException(
                bo.com.ganadero.shared.error.ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public UUID id() { return id; } public String codigo() { return codigo; }
    public String razonSocial() { return razonSocial; } public String nombreComercial() { return nombreComercial; }
    public String nit() { return nit; } public String telefono() { return telefono; }
    public String email() { return email; } public String direccion() { return direccion; }
    public String zonaHoraria() { return zonaHoraria; } public String moneda() { return moneda; }
    public EstadoEmpresa estado() { return estado; } public String logoPath() { return logoPath; }
    public Instant createdAt() { return createdAt; } public UUID createdBy() { return createdBy; }
    public Instant updatedAt() { return updatedAt; } public UUID updatedBy() { return updatedBy; }
    public long version() { return version; }
}
