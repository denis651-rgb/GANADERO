package bo.com.ganadero.empresas.infrastructure;

import bo.com.ganadero.empresas.domain.Empresa;
import bo.com.ganadero.empresas.domain.EstadoEmpresa;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresas", schema = "core")
class EmpresaJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private String codigo;
    @Column(name = "razon_social", nullable = false) private String razonSocial;
    @Column(name = "nombre_comercial", nullable = false) private String nombreComercial;
    private String nit;
    private String telefono;
    private String email;
    private String direccion;
    @Column(name = "zona_horaria", nullable = false) private String zonaHoraria;
    @Column(nullable = false) private String moneda;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoEmpresa estado;
    @Column(name = "logo_path") private String logoPath;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "created_by", updatable = false) private UUID createdBy;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by") private UUID updatedBy;
    @Version private long version;

    protected EmpresaJpaEntity() {}

    Empresa toDomain() {
        return new Empresa(id, codigo, razonSocial, nombreComercial, nit, telefono, email, direccion,
                zonaHoraria, moneda, estado, logoPath, createdAt, createdBy, updatedAt, updatedBy, version);
    }

    void apply(Empresa empresa) {
        razonSocial = empresa.razonSocial(); nombreComercial = empresa.nombreComercial();
        nit = empresa.nit(); telefono = empresa.telefono(); email = empresa.email();
        direccion = empresa.direccion(); updatedBy = empresa.updatedBy(); updatedAt = Instant.now();
    }
}
