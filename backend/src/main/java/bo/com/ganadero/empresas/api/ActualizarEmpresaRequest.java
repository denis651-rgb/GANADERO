package bo.com.ganadero.empresas.api;

import bo.com.ganadero.empresas.application.ActualizarEmpresaCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarEmpresaRequest(
        @Size(max = 180) String razonSocial,
        @Size(max = 180) String nombreComercial,
        @Size(max = 40) String nit,
        @Size(max = 40) String telefono,
        @Email @Size(max = 180) String email,
        @Size(max = 300) String direccion,
        @NotNull Long version) {
    ActualizarEmpresaCommand toCommand() {
        return new ActualizarEmpresaCommand(razonSocial, nombreComercial, nit, telefono, email, direccion, version);
    }
}
