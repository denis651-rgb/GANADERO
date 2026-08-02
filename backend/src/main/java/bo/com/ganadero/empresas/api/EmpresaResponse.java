package bo.com.ganadero.empresas.api;

import bo.com.ganadero.empresas.domain.Empresa;
import bo.com.ganadero.empresas.domain.EstadoEmpresa;

import java.time.Instant;
import java.util.UUID;

public record EmpresaResponse(UUID id, String codigo, String razonSocial, String nombreComercial,
        String nit, String telefono, String email, String direccion, String zonaHoraria, String moneda,
        EstadoEmpresa estado, String logoPath, Instant createdAt, UUID createdBy, Instant updatedAt,
        UUID updatedBy, long version) {
    static EmpresaResponse from(Empresa source) {
        return new EmpresaResponse(source.id(), source.codigo(), source.razonSocial(), source.nombreComercial(),
                source.nit(), source.telefono(), source.email(), source.direccion(), source.zonaHoraria(),
                source.moneda(), source.estado(), source.logoPath(), source.createdAt(), source.createdBy(),
                source.updatedAt(), source.updatedBy(), source.version());
    }
}
